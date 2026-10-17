# Como trabalhar neste repositório

Mesmas práticas do [KDS Brasa do Jorge](https://github.com/gabrielevanger/kds-brasa-do-jorge): passos pequenos,
cada mudança ligada a uma issue e a `main` sempre verde.

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

## Antes de abrir o PR

```bash
./gradlew ktlintCheck :app:lintDebug test
```

Se mexeu em `supabase/`:

```bash
cd supabase/tests && npm install && npm test
```
