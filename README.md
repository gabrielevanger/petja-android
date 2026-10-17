# Xodó Pet

[![CI](https://github.com/gabrielevanger/xodo-pet-android/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/gabrielevanger/xodo-pet-android/actions/workflows/ci.yml)

Delivery de produtos para pets em Boa Vista (RR): uma vitrine com os petshops da cidade, no estilo do Zé Delivery.
Projeto pessoal, por enquanto com dados fictícios.

- **Requisitos:** [REQUISITOS.md](REQUISITOS.md)
- **Banco (Supabase):** [supabase/README.md](supabase/README.md)
- **O que falta fazer:** [issues por fase](https://github.com/gabrielevanger/xodo-pet-android/milestones)
- **Como contribuir:** [CONTRIBUTING.md](CONTRIBUTING.md)

## Estrutura

Mesma organização do [KDS Brasa do Jorge](https://github.com/gabrielevanger/kds-brasa-do-jorge):

```
app .................. MainActivity, configuração do Supabase
feature/storefront ... vitrine: lojas que entregam no endereço (ou petshops não parceiros)
core/ui .............. formatação (R$, distância) e links de contato
core/designsystem .... tema do Xodó (paleta do lavrado), claro e escuro
core/data ............ Supabase (PostgREST via supabase-kt), DTOs e mapeamento
core/domain .......... Kotlin puro: modelos e regras da vitrine
build-logic .......... plugins de convenção do Gradle
supabase ............. migrações, seed, RLS e testes do banco
```

Dependências: `app -> feature/* -> core/ui, core/designsystem, core/domain`; `core/data -> core/domain`;
`core/domain` não depende de nada do Android.

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

## Verificar

```bash
./gradlew ktlintCheck :app:lintDebug test
```

Testes do banco (Node, sem Docker):

```bash
cd supabase/tests && npm install && npm test
```

O CI (`.github/workflows/ci.yml`) roda os dois e, em pull requests, confere as mensagens de commit.

## Commits

Conventional Commits em português, validados por hook e no CI. Detalhes e fluxo de branch e PR em
[CONTRIBUTING.md](CONTRIBUTING.md).
