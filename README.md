<h1 align="center">PetJá</h1>

<p align="center">
  <a href="https://github.com/gabrielevanger/petja-android/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/gabrielevanger/petja-android/actions/workflows/ci.yml/badge.svg?branch=main"/></a>
  <a href="https://developer.android.com/tools/releases/platforms"><img alt="API" src="https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://kotlinlang.org"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4-7F52FF.svg?style=flat&logo=kotlin&logoColor=white"/></a>
  <a href="https://developer.android.com/compose"><img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat&logo=jetpackcompose&logoColor=white"/></a>
</p>

<p align="center">
Delivery de produtos para pets em Boa Vista (RR): uma vitrine com os petshops da cidade, em que o cliente
informa onde está, vê as lojas que entregam ali, monta o carrinho e recebe em casa.
</p>

<p align="center">
  <img src="feature/storefront/src/test/snapshots/storefront_loaded.png" width="260" alt="Vitrine com os petshops que entregam no bairro">
  &nbsp;&nbsp;
  <img src="feature/storefront/src/test/snapshots/storefront_loading.png" width="260" alt="Vitrine procurando petshops perto do cliente">
</p>

<p align="center"><sub>Imagens geradas pelos screenshot tests (Roborazzi): se a tela mudar sem querer, o CI falha.</sub></p>

## Stack e bibliotecas

- Android com **minSdk 26** e **targetSdk 37**, 100% [Kotlin](https://kotlinlang.org), com
  [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) e
  [Flow](https://kotlinlang.org/docs/flow.html) para tudo que é assíncrono.
- Jetpack:
  - [Compose](https://developer.android.com/compose) com [Material 3](https://m3.material.io): toda a interface, sem XML de layout.
  - [Lifecycle e ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel): estado da tela que sobrevive à rotação e coleta de Flow ligada ao ciclo de vida.
  - [Navigation](https://developer.android.com/guide/navigation/design/type-safety) com rotas tipadas.
  - [DataStore](https://developer.android.com/topic/libraries/architecture/datastore): preferências locais.
  - [Hilt](https://dagger.dev/hilt): injeção de dependências.
- Arquitetura:
  - Fluxo unidirecional de dados (UDF): `Action`, `UiState` imutável e `Event` por tela.
  - Módulos por camada e por funcionalidade, seguindo o [guia oficial](https://developer.android.com/topic/modularization).
  - Plugins de convenção do Gradle em `build-logic` e versões no [catálogo](gradle/libs.versions.toml).
- [Supabase](https://supabase.com) via [supabase-kt](https://github.com/supabase-community/supabase-kt):
  - Postgrest para os dados, Auth com PKCE para a conta e Realtime para os pedidos ao vivo.
  - PostgreSQL com RLS em todas as tabelas e PostGIS para a área de entrega.
- [Ktor](https://ktor.io) (motor OkHttp) e [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization) por baixo do supabase-kt.
- [kotlinx-collections-immutable](https://github.com/Kotlin/kotlinx.collections.immutable): listas imutáveis no estado da tela.

## Funcionalidades

| Para quem | O que já faz |
|---|---|
| **Cliente** | Vitrine por bairro com as lojas que entregam ali, página da loja com categorias e produtos, carrinho com taxa de entrega e pedido mínimo, checkout com pagamento na entrega e acompanhamento do pedido ao vivo |
| **Conta** | Entrar, criar conta, recuperar e trocar senha |
| **Lojista** | Modo loja com os pedidos chegando ao vivo, alerta sonoro e avanço das etapas do pedido |

Projeto pessoal, por enquanto com dados fictícios. O plano completo está em [REQUISITOS.md](REQUISITOS.md)
e o que falta fazer está nas [issues por fase](https://github.com/gabrielevanger/petja-android/milestones).

## Arquitetura

Modularização por camada e por funcionalidade, como recomenda o
[guia de modularização do Android](https://developer.android.com/topic/modularization):

```
app .................. MainActivity, navegação e configuração do Supabase
feature/storefront ... vitrine: lojas que entregam no endereço (ou petshops não parceiros)
feature/store ........ página da loja, com categorias e produtos
feature/cart ......... carrinho, taxa de entrega e pedido mínimo
feature/checkout ..... endereço, pagamento na entrega e pedido enviado
feature/orders ....... meus pedidos e acompanhamento ao vivo
feature/account ...... entrar, criar conta, senha e conta
feature/storemode .... modo lojista: pedidos da loja ao vivo, etapas e alerta sonoro
core/ui .............. formatação (R$, distância), links de contato e coleta de eventos
core/designsystem .... tema do PetJá (paleta do lavrado), claro e escuro, e componentes
core/data ............ Supabase (PostgREST, Auth e Realtime via supabase-kt), DTOs e mapeamento
core/domain .......... Kotlin puro: modelos e regras de negócio
build-logic .......... plugins de convenção do Gradle
supabase ............. migrações, seed, RLS e testes do banco
```

```mermaid
flowchart LR
    app --> feature["feature/*"]
    feature --> ui["core/ui"]
    feature --> ds["core/designsystem"]
    feature --> domain["core/domain"]
    app --> data["core/data"]
    data --> domain
```

`core/domain` não depende de nada do Android (um teste do Konsist garante isso).

Cada tela segue **fluxo unidirecional de dados (UDF)**: a tela envia `Action` para o `ViewModel`, que publica
um `UiState` imutável e eventos únicos (`Event`, como navegação e snackbar). A `Route` liga o ViewModel e a
`Screen` só desenha. Os detalhes estão em [CONTRIBUTING.md](CONTRIBUTING.md#telas-e-viewmodels).

## Rodar o app

1. Crie o projeto no Supabase e aplique o banco (passo a passo em [supabase/README.md](supabase/README.md)).
2. Em *Project Settings → API*, copie a **Project URL** e a chave **anon public**.
3. Coloque no `local.properties` da raiz (o arquivo não vai para o Git):

```properties
sdk.dir=/caminho/do/Android/Sdk
supabase.url=https://SEU-PROJETO.supabase.co
supabase.anonKey=SUA_CHAVE_ANON
```

4. Abra no Android Studio e rode o `app`, ou:

```bash
./gradlew :app:installDebug
```

Sem `supabase.url`, o app compila e abre, mas a vitrine mostra o erro de conexão.

A chave **anon** é pública por natureza: quem protege os dados é a RLS do banco. Nunca coloque a
chave `service_role` no app.

## Qualidade

Tudo abaixo roda no CI em cada pull request, e a `main` só aceita merge com os três checks verdes.

| Verificação | Ferramenta | O que garante |
|---|---|---|
| Testes de ViewModel e domínio | [JUnit](https://junit.org), [Turbine](https://github.com/cashapp/turbine) e coroutines-test | Cada ação muda o estado do jeito certo |
| Testes de tela | [Robolectric](https://robolectric.org) e Compose Test | As telas funcionam na JVM, sem emulador |
| Screenshot tests | [Roborazzi](https://github.com/takahirom/roborazzi) | Nenhuma tela muda de aparência sem querer |
| Testes de arquitetura | [Konsist](https://docs.konsist.lemonappdev.com) | Domínio sem Android, ViewModels com Hilt, UiState imutável, Screen sem ViewModel |
| Cobertura | [Kover](https://github.com/Kotlin/kotlinx-kover) | Mínimo de 80% por módulo e 90% no projeto |
| Estilo e análise estática | [ktlint](https://pinterest.github.io/ktlint) e [Android Lint](https://developer.android.com/studio/write/lint) | Código padronizado e sem problemas conhecidos do Android |
| Design tokens | `scripts/check-ui-tokens.sh` | Nenhuma cor, medida ou texto solto fora do tema |
| Banco | Node (`supabase/tests`) | Migrações, seed e regras de RLS |
| Commits | hook e CI ([Conventional Commits](https://www.conventionalcommits.org/pt-br)) | Conventional Commits em português |

Para rodar localmente:

```bash
./gradlew ktlintCheck :app:lintDebug test
```

```bash
./gradlew koverHtmlReportCoverage koverVerifyCoverage
```

```bash
./scripts/record-screenshots.sh
```

```bash
cd supabase/tests && npm install && npm test
```

O relatório de cobertura fica em `build/reports/kover/htmlCoverage/index.html`. Depois de mudar uma tela de
propósito, o `record-screenshots.sh` regrava as imagens de referência: confira e commite junto.

## Contribuir

Conventional Commits em português, validados por hook e no CI. Fluxo de branch, PR e padrões de código em
[CONTRIBUTING.md](CONTRIBUTING.md). O banco tem documentação própria em [supabase/README.md](supabase/README.md).
