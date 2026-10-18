# Como trabalhar neste repositório

O fluxo segue padrões consolidados no mercado: [GitHub Flow](https://docs.github.com/pt/get-started/using-github/github-flow)
(branches curtas a partir da `main`, integradas por pull request), [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/)
nas mensagens e integração contínua obrigatória antes do merge. Na prática: passos pequenos, cada mudança ligada
a uma issue e a `main` sempre verde.

## Fluxo

1. **Issue primeiro.** Todo trabalho tem uma issue, com o marco da fase (REQUISITOS.md, seção 12)
   e os códigos dos requisitos (RF, RN, RNF).
2. **Branch a partir da `main`**, com o tipo do trabalho no nome: `feat/pagina-da-loja`, `fix/taxa-de-entrega`,
   `docs/readme`, `ci/cache-do-gradle`, `refactor/...`, `test/...`, `build/...`, `chore/...`.
3. **Commits pequenos**, cada um com uma ideia só e o projeto compilando.
4. **Pull request** com o modelo do repositório (Por quê, O que muda, Como foi verificado) e
   `Closes #N` para fechar a issue no merge.
5. **CI verde** antes do merge: ktlint, Android Lint, testes do app, testes do banco e mensagens de commit.
6. **Merge commit**, sem squash nem rebase, para preservar o histórico de cada passo.

## Mensagens de commit

[Conventional Commits](https://www.conventionalcommits.org/pt-br/) em português, no imperativo da 3ª pessoa:

```
<tipo>(<escopo opcional>): <descrição>

<corpo opcional: o porquê, não o como>
```

- **Tipos:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`
- **Escopos usados:** `app`, `vitrine`, `domain`, `data`, `designsystem`, `ui`, `banco`, `requisitos`, `readme`
- **Título:** até 72 caracteres, sem ponto final
- **Sem travessão (—) nem meia-risca (–):** use vírgula, dois-pontos ou parênteses
- Trabalho feito com IA leva o rodapé `Co-Authored-By` da IA

Exemplos:

```
feat(vitrine): mostra petshops não parceiros quando nenhuma loja entrega
fix(banco): impede cliente de adicionar membros à equipe da loja
test(domain): cobre a ordem das lojas abertas e fechadas
```

O hook `.githooks/commit-msg` valida as regras antes de cada commit. Ative uma vez por clone:

```bash
git config core.hooksPath .githooks
```

O CI roda o mesmo hook em todos os commits de um PR, então pular com `--no-verify` não adianta.

## Interface: tokens e textos

Telas não declaram valores soltos. Tudo vem do `core/designsystem` e dos arquivos `strings.xml`:

| Em vez de | Use |
|---|---|
| `16.dp`, `12.sp` | `XodoTheme.spacing.m`, `XodoTheme.sizes.*`, estilos de `XodoTheme.typography` |
| `MaterialTheme.typography.bodySmall`, `FontWeight.Bold` | o papel do texto: `XodoTheme.typography.caption`, `.price`, `.sectionTitle`... |
| `Color(0xFF...)`, `MaterialTheme.colorScheme` | `XodoTheme.colors.primary`, `.onSurfaceVariant`... |
| `Text("Tentar de novo")` | `stringResource(R.string....)`; textos usados em várias telas ficam em `core/ui` |
| `joinToString(" · ")` | `dotSeparated(...)` |
| Selo, título de seção, carregando, erro | `StatusBadge`, `SectionTitle`, `LoadingState`, `ErrorState`, `MessageState`, `Notice` |

Número que não é medida de tela (limite, quantidade, conversão) vira constante com nome e comentário.

Os tokens ficam em Kotlin, e não em `res/values/dimens.xml`: no Compose eles são tipados, entram no tema
e trocam junto com ele. O `scripts/check-ui-tokens.sh` confere a regra localmente e no CI; previews
(`*Preview.kt`) e testes ficam de fora.

## Telas e ViewModels

Cada tela tem o seu conjunto de arquivos. Módulos com mais de uma tela usam um subpacote por tela
(`orders/list`, `orders/details`). Exemplo: `feature/storefront`.

```
StorefrontUiState.kt    estado da tela, inclusive o carregando e o erro
StorefrontAction.kt     o que a tela pede: OnStart, OnStop, OnPlaceClick, OnStoreClick...
StorefrontEvent.kt      o que acontece uma vez só: navegar, tocar som, mostrar snackbar
StorefrontViewModel.kt  recebe Action, atualiza o UiState e manda Event
StorefrontScreen.kt     Route (liga ViewModel, ciclo de vida e navegação) e Screen (só desenha)
```

1. **Entrada única.** A tela chama só `viewModel.onAction(...)`, e o ViewModel decide com um `when` exaustivo.
   A `Screen` recebe `state` e `onAction`, o que facilita os testes e os previews.
2. **A carga começa pela tela.** A `Route` manda `OnStart` e `OnStop` com `LifecycleEventEffect`.
   Carga única tem uma guarda (`started`) para a rotação não repetir a busca. O que fica escutando
   (Realtime, carrinho) liga no `OnStart` e cancela o `Job` no `OnStop`.
3. **Cada carga é uma corrente sobre um `Flow`:**
   ```kotlin
   loading?.cancel()
   loading = flow { emit(loadStorefront(point)) }
       .onStart { _uiState.update { it.copy(content = Loading) } }
       .onEach { content -> _uiState.update { it.copy(content = content) } }
       .catch { _uiState.update { it.copy(content = Error) } } // sempre por último: só pega o que vem antes
       .launchIn(viewModelScope)
   ```
   O `Job` fica guardado e é cancelado antes de começar outra carga igual, para uma resposta velha não
   sobrescrever a nova.
4. **Um estado só.** `MutableStateFlow` privado, exposto com `asStateFlow()` e alterado com `update`.
   Carregando, erro e "enviando" ficam no estado, nunca como evento.
5. **Eventos** vão por `Channel(Channel.BUFFERED)` com `receiveAsFlow()`. A `Route` coleta com
   `CollectEvents` (de `core/ui`), que só entrega com a tela visível e não perde o evento enquanto ela não está.
6. **Cargas em paralelo** usam `coroutineScope { async { } ... awaitAll() }`.
7. **Testes do ViewModel** mandam `OnStart` como a tela faz, depois as Actions, e conferem o `uiState` e os
   `events` (com Turbine).

## Cobertura de testes

O Kover mede a cobertura dos testes unitários e o CI falha abaixo destes pisos:

| Onde | Linhas | Ramos |
|---|---:|---:|
| **Todo módulo** (medido só com os testes dele) | **80%** | sem piso |
| Projeto inteiro | 90% | sem piso |
| `core/domain` (regras de negócio) | 95% | 70% |
| `core/data` | 90% | sem piso |

O mínimo de 80% vale por módulo e conta só os testes do próprio módulo: um componente do design system
usado por uma tela precisa de teste no `core/designsystem`, não só no teste da tela. O módulo `app` fica fora,
porque só liga as peças.

Ficam fora da conta o código gerado ou só de ligação: Hilt, `BuildConfig`, `R`, módulos de DI, o módulo `app`,
previews, rotas de navegação e DTOs (ver `build-logic/.../KoverConfiguration.kt`). Ramos de telas Compose não têm
piso porque o compilador do Compose gera ramos internos que nenhum teste percorre.

Módulo novo já nasce com o mínimo de 80% e entra no relatório geral com `kover(projects....)` no
`build.gradle.kts` da raiz. Piso acima do mínimo: `minimumCoverage(lines = ..., branches = ...)` no
`build.gradle.kts` do módulo.

## Antes de abrir o PR

```bash
scripts/check-ui-tokens.sh
```

```bash
./gradlew ktlintCheck :app:lintDebug test koverVerifyCoverage
```

Se mexeu em `supabase/`:

```bash
cd supabase/tests && npm install && npm test
```
