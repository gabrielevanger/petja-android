# Xodó Pet: banco de dados (Supabase)

Postgres com PostGIS, regras de negócio em funções e segurança por Row Level Security.
Os requisitos citados (RF, RN, RNF) estão em [REQUISITOS.md](../REQUISITOS.md).

```
supabase/
  migrations/
    ..._schema.sql                  tabelas, tipos e índices
    ..._functions.sql               regras de negócio (RPC), gatilhos e permissões
    ..._rls.sql                     políticas de Row Level Security
    ..._realtime_storage_cron.sql   Realtime, bucket de fotos e cancelamento automático
  seed.sql                          dados fictícios de Boa Vista
  dev/claim_store.sql               liga a sua conta a uma loja (admin + dono)
  dev/advance_order.sql             anda um pedido para a próxima etapa, como a loja faria
  tests/                            testes das regras e da RLS, sem precisar de Supabase
```

## Como aplicar

### Opção A: projeto na nuvem (mais simples)
1. Crie um projeto em [supabase.com](https://supabase.com) (plano grátis), na região **São Paulo**.
2. No **SQL Editor**, rode os arquivos de `migrations/` **na ordem**, depois o `seed.sql`.
3. Cadastre-se pelo app (ou em *Authentication → Users → Add user*) e rode o `dev/claim_store.sql` com o seu e-mail.

### Opção B: Supabase CLI
```bash
supabase init
```
(mantém a pasta `migrations/` e o `seed.sql` que já estão aqui)

```bash
supabase link --project-ref SEU_PROJECT_REF
```

```bash
supabase db push --include-seed
```

Localmente, com Docker, o `supabase start` seguido de `supabase db reset` aplica migrações e seed.

### Testes (sem Supabase nem Docker)
Rodam as migrações e o seed num Postgres com PostGIS em memória (PGlite) e conferem 66 regras:
vitrine por raio, pedido, máquina de estados, estoque, rastreio, cancelamento automático e RLS.

```bash
cd supabase/tests && npm install && npm test
```

O PGlite imita só o essencial do Supabase (schema `auth`, papéis `anon` e `authenticated`).
Realtime, Storage e pg_cron são pulados nos testes e só funcionam no Supabase.

## Modelo

| Tabela | O que guarda |
|---|---|
| `profiles` | Um por usuário do Auth, criado por gatilho no cadastro. `is_admin` só muda pelo admin |
| `addresses` | Endereços do cliente, com o pino do mapa (`location`) |
| `stores` | Lojas parceiras, com localização, raio e regras de taxa. `is_active` só muda pelo admin |
| `store_members` | Equipe da loja: `owner`, `staff` ou `courier` |
| `external_stores` | Petshops não parceiros, para o RF-15 (fase mockada) |
| `categories`, `products`, `product_variants` | Catálogo. Preço e estoque ficam na variante (ex.: 1 kg, 15 kg) |
| `orders`, `order_items` | Pedido com cópia de endereço, nomes e preços (RN-03) |
| `order_status_history` | Cada mudança de status, quem fez e o motivo |
| `courier_locations` | Última posição do entregador, só durante a entrega (RN-11) |

Dinheiro sempre em **centavos** (`integer`). Localizações em `geography(Point, 4326)`.

## API para o app

O app lê as tabelas direto (a RLS filtra o que cada um vê) e **escreve pedidos só pelas funções**,
que validam as regras antes de gravar. No Kotlin: `supabase.postgrest.rpc("nome", parametros)`.

| Função | Quem chama | O que faz |
|---|---|---|
| `stores_near(p_lat, p_lng)` | qualquer um | Lojas que entregam no ponto, abertas primeiro, com distância e taxa (RF-20) |
| `external_stores_near(p_lat, p_lng, p_limit, p_max_km)` | qualquer um | Petshops não parceiros mais próximos (RF-15) |
| `place_order(p_store_id, p_address_id, p_items, p_payment_method, p_change_for_cents, p_notes, p_expected_total_cents)` | cliente | Cria o pedido, baixa o estoque. `p_items`: `[{"variant_id": "...", "quantity": 2}]` |
| `transition_order(p_order_id, p_to, p_reason)` | cliente, loja, entregador | Muda o status conferindo a máquina de estados e a permissão de cada um |
| `assign_courier(p_order_id, p_courier_id)` | loja | Atribui um entregador da equipe (RF-91) |
| `update_courier_location(p_order_id, p_lat, p_lng, p_heading, p_speed_mps)` | entregador do pedido | Envia a posição durante a entrega (RF-113) |
| `add_store_member(p_store_id, p_email, p_role)` | dono | Adiciona alguém já cadastrado à equipe (RF-110) |

### Quem pode mudar o status
| Quem | Transições |
|---|---|
| Cliente | `pending → canceled` |
| Dono ou atendente | Qualquer transição válida |
| Entregador do pedido | `preparing → out_for_delivery`, `out_for_delivery → delivered / not_delivered` |
| Admin | Qualquer transição válida |

Recusar, cancelar (pela loja) e "não entregue" exigem motivo.

### Erros
A mensagem do erro é um código estável para o app traduzir; o `detail` traz o texto em português.

| Código | Quando |
|---|---|
| `not_authenticated` | Sem login |
| `forbidden` | Sem permissão para a ação |
| `address_not_found`, `store_not_found`, `order_not_found` | Registro inexistente ou de outra pessoa |
| `store_closed` | Loja fechada |
| `out_of_delivery_area` | Endereço fora do raio da loja |
| `empty_cart`, `invalid_quantity` | Carrinho vazio ou quantidade inválida |
| `item_unavailable` | Item pausado, removido ou de outra loja |
| `insufficient_stock` | Estoque menor que a quantidade |
| `below_min_order` | Abaixo do pedido mínimo |
| `price_changed` | Total diferente do que o app mostrou (RF-45) |
| `invalid_change` | Troco em pagamento que não é dinheiro |
| `invalid_transition` | Mudança de status fora da máquina de estados |
| `reason_required` | Faltou o motivo |
| `courier_not_found`, `user_not_found` | Entregador ou e-mail não encontrado |

## Tempo real
- **Status do pedido:** o app escuta `orders` com `postgresChanges`. O Realtime respeita a RLS.
- **Entregador no mapa:** o app do cliente escuta `courier_locations` filtrando pelo `order_id`.
  Cada envio do entregador sobrescreve a mesma linha, então a tabela fica sempre pequena.
  Se o volume crescer, dá para trocar por *Broadcast* do Realtime sem mudar o resto.

## Cancelamento automático (RN-06)
`private.expire_pending_orders(10)` cancela pedidos pendentes há mais de 10 minutos e devolve o estoque.
No Supabase, o pg_cron roda a função a cada minuto (a migração agenda sozinha se o pg_cron
estiver disponível; se não estiver, ative em *Database → Extensions* e rode a migração 4 de novo).

## Fotos
Bucket público `store-media`, até 2 MB por imagem (JPEG, PNG ou WebP).
Caminho: `<store_id>/<arquivo>`. Só a equipe da loja envia e apaga na pasta da loja.

## Dados fictícios
O `seed.sql` cria 7 categorias, 6 lojas (uma fechada e uma não aprovada), 5 petshops não parceiros
e cerca de 100 produtos com 190 variantes. Lojas, telefones e endereços são inventados; as
coordenadas caem em bairros reais de Boa Vista, mas são aproximadas.

Pontos bons para testar a vitrine:

| Lugar | Latitude, longitude | O que acontece |
|---|---|---|
| Centro | 2.8200, -60.6720 | Várias lojas entregam |
| Buritis | 2.8230, -60.7150 | Várias lojas, uma delas fechada |
| Cidade Satélite | 2.8600, -60.7600 | Nenhuma parceira: aparecem os petshops não parceiros |
