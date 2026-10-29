<div align="center">

# PetJá

**Delivery de produtos para pets em Boa Vista (RR)**

Uma vitrine com os petshops da cidade: o cliente informa onde está, vê as lojas que entregam ali,
monta o carrinho e recebe em casa.

[![CI](https://github.com/gabrielevanger/petja-android/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/gabrielevanger/petja-android/actions/workflows/ci.yml)
[![Supabase ativo](https://github.com/gabrielevanger/petja-android/actions/workflows/keep-alive.yml/badge.svg)](https://github.com/gabrielevanger/petja-android/actions/workflows/keep-alive.yml)
[![Último commit](https://img.shields.io/github/last-commit/gabrielevanger/petja-android?label=%C3%BAltimo%20commit)](https://github.com/gabrielevanger/petja-android/commits/main)
[![Issues abertas](https://img.shields.io/github/issues/gabrielevanger/petja-android?label=issues)](https://github.com/gabrielevanger/petja-android/issues)

<br>

**Plataforma e linguagem**

[![Android](https://img.shields.io/badge/Android-minSdk%2026%20%7C%20targetSdk%2037-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.8-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![AGP](https://img.shields.io/badge/AGP-9.4.1-3DDC84?logo=android&logoColor=white)](https://developer.android.com/build)

**Interface e arquitetura**

[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.09-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Material 3](https://img.shields.io/badge/Material%203-design%20system-757575?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Hilt](https://img.shields.io/badge/Hilt-2.60.1-34A853?logo=android&logoColor=white)](https://dagger.dev/hilt)
[![Navigation](https://img.shields.io/badge/Navigation-2.10.2-34A853?logo=android&logoColor=white)](https://developer.android.com/guide/navigation)
[![Coroutines](https://img.shields.io/badge/Coroutines%20e%20Flow-1.11.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/coroutines-overview.html)
[![Serialization](https://img.shields.io/badge/Serialization-1.11.0-7F52FF?logo=kotlin&logoColor=white)](https://github.com/Kotlin/kotlinx.serialization)

**Back-end**

[![Supabase](https://img.shields.io/badge/Supabase-supabase--kt%203.8.0-3FCF8E?logo=supabase&logoColor=white)](https://supabase.com)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-RLS%20e%20PostGIS-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.5.1-087CFA?logo=ktor&logoColor=white)](https://ktor.io)
[![Realtime](https://img.shields.io/badge/Realtime-pedidos%20ao%20vivo-3FCF8E?logo=supabase&logoColor=white)](https://supabase.com/docs/guides/realtime)

**Testes e qualidade**

[![JUnit](https://img.shields.io/badge/JUnit-6.1.3-25A162?logo=junit5&logoColor=white)](https://junit.org)
[![Robolectric](https://img.shields.io/badge/Robolectric-4.17-3DDC84?logo=android&logoColor=white)](https://robolectric.org)
[![Turbine](https://img.shields.io/badge/Turbine-1.2.1-7F52FF?logo=kotlin&logoColor=white)](https://github.com/cashapp/turbine)
[![Roborazzi](https://img.shields.io/badge/Roborazzi-1.76.0-FF6F00?logo=android&logoColor=white)](https://github.com/takahirom/roborazzi)
[![Konsist](https://img.shields.io/badge/Konsist-0.16.1-7F52FF?logo=kotlin&logoColor=white)](https://docs.konsist.lemonappdev.com)
[![Kover](https://img.shields.io/badge/Kover-cobertura%20m%C3%ADnima%2080%25-7F52FF?logo=kotlin&logoColor=white)](https://github.com/Kotlin/kotlinx-kover)
[![ktlint](https://img.shields.io/badge/ktlint-1.8.0-FF4088?logo=kotlin&logoColor=white)](https://pinterest.github.io/ktlint)
[![Android Lint](https://img.shields.io/badge/Android%20Lint-ativo-3DDC84?logo=android&logoColor=white)](https://developer.android.com/studio/write/lint)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-pt--BR-FE5196?logo=conventionalcommits&logoColor=white)](https://www.conventionalcommits.org/pt-br)

</div>

---

## Telas

<p align="center">
  <img src="feature/storefront/src/test/snapshots/storefront_loaded.png" width="260" alt="Vitrine com os petshops que entregam no bairro">
  &nbsp;&nbsp;
  <img src="feature/storefront/src/test/snapshots/storefront_loading.png" width="260" alt="Vitrine procurando petshops perto do cliente">
</p>

<p align="center"><sub>Imagens geradas pelos screenshot tests (Roborazzi): se a tela mudar sem querer, o CI falha.</sub></p>

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
| Testes de ViewModel e domínio | JUnit, Turbine, coroutines-test | Cada ação muda o estado do jeito certo |
| Testes de tela | Robolectric e Compose Test | As telas funcionam na JVM, sem emulador |
| Screenshot tests | Roborazzi | Nenhuma tela muda de aparência sem querer |
| Testes de arquitetura | Konsist | Domínio sem Android, ViewModels com Hilt, UiState imutável, Screen sem ViewModel |
| Cobertura | Kover | Mínimo de 80% por módulo e 90% no projeto |
| Estilo e análise estática | ktlint e Android Lint | Código padronizado e sem problemas conhecidos do Android |
| Design tokens | `scripts/check-ui-tokens.sh` | Nenhuma cor, medida ou texto solto fora do tema |
| Banco | Node (`supabase/tests`) | Migrações, seed e regras de RLS |
| Commits | hook e CI | Conventional Commits em português |

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
