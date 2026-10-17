-- Xodó Pet: tabelas, tipos e índices.
-- Valores em dinheiro são inteiros em centavos (R$ 12,90 = 1290), para não haver erro de arredondamento.
-- Localizações são geography(Point, 4326): latitude/longitude em graus, distâncias em metros.

create extension if not exists postgis with schema extensions;

-- ---------------------------------------------------------------------------
-- Tipos
-- ---------------------------------------------------------------------------

-- Máquina de estados do pedido (REQUISITOS.md, seção 4.1).
create type public.order_status as enum (
    'pending',          -- PENDENTE: aguardando a loja aceitar
    'accepted',         -- ACEITO
    'preparing',        -- EM_SEPARACAO
    'out_for_delivery', -- SAIU_PARA_ENTREGA
    'delivered',        -- ENTREGUE (final)
    'not_delivered',    -- NAO_ENTREGUE (final)
    'rejected',         -- RECUSADO pela loja (final)
    'canceled'          -- CANCELADO (final)
);

create type public.payment_method as enum (
    'cash',             -- dinheiro na entrega
    'pix_on_delivery',  -- Pix na entrega
    'card_on_delivery'  -- cartão na maquininha
);

create type public.store_role as enum (
    'owner',   -- dono: tudo na loja, inclusive gerenciar a equipe
    'staff',   -- atendente: pedidos e catálogo
    'courier'  -- entregador: só as entregas atribuídas a ele
);

create type public.pet_species as enum (
    'dog', 'cat', 'bird', 'fish', 'rodent', 'reptile', 'other'
);

-- ---------------------------------------------------------------------------
-- Pessoas
-- ---------------------------------------------------------------------------

-- Uma linha por usuário do Supabase Auth, criada pelo gatilho on_auth_user_created.
create table public.profiles (
    id         uuid primary key references auth.users (id) on delete cascade,
    full_name  text,
    phone      text,
    is_admin   boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.addresses (
    id           uuid primary key default gen_random_uuid(),
    profile_id   uuid not null references public.profiles (id) on delete cascade,
    label        text,                       -- "Casa", "Trabalho"
    street       text not null,
    number       text not null,
    complement   text,
    neighborhood text not null,
    reference    text,                       -- ponto de referência para o entregador
    city         text not null default 'Boa Vista',
    state        text not null default 'RR',
    location     extensions.geography (point, 4326) not null, -- pino confirmado no mapa (RF-11)
    is_default   boolean not null default false,
    created_at   timestamptz not null default now()
);

create index addresses_profile_idx on public.addresses (profile_id);
-- No máximo um endereço padrão por pessoa.
create unique index addresses_one_default_idx on public.addresses (profile_id) where is_default;

-- ---------------------------------------------------------------------------
-- Lojas
-- ---------------------------------------------------------------------------

create table public.stores (
    id                 uuid primary key default gen_random_uuid(),
    name               text not null,
    slug               text not null unique,
    description        text,
    logo_url           text,
    phone              text,
    whatsapp           text,
    street             text not null,
    number             text not null,
    neighborhood       text not null,
    location           extensions.geography (point, 4326) not null,
    is_active          boolean not null default false, -- aprovada pelo admin (RF-100)
    is_open            boolean not null default false, -- aberta agora (RF-84)
    min_order_cents    integer not null default 0 check (min_order_cents >= 0),
    estimated_minutes  integer not null default 45 check (estimated_minutes > 0),
    -- Entrega (RN-07): taxa base até included_km, mais extra_km_fee_cents por km além disso,
    -- até delivery_radius_km.
    delivery_radius_km numeric(4, 1) not null default 5 check (delivery_radius_km > 0),
    base_fee_cents     integer not null default 500 check (base_fee_cents >= 0),
    included_km        numeric(4, 1) not null default 2 check (included_km >= 0),
    extra_km_fee_cents integer not null default 150 check (extra_km_fee_cents >= 0),
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now()
);

create index stores_location_idx on public.stores using gist (location);

create table public.store_members (
    store_id   uuid not null references public.stores (id) on delete cascade,
    profile_id uuid not null references public.profiles (id) on delete cascade,
    role       public.store_role not null,
    created_at timestamptz not null default now(),
    primary key (store_id, profile_id)
);

create index store_members_profile_idx on public.store_members (profile_id);

-- Petshops que não são parceiros, mostrados quando nenhuma parceira entrega no endereço (RF-15).
-- Usada na fase mockada; com dados reais, a busca será feita na hora (Google Places ou OSM).
create table public.external_stores (
    id           uuid primary key default gen_random_uuid(),
    name         text not null,
    phone        text,
    whatsapp     text,
    street       text,
    neighborhood text,
    location     extensions.geography (point, 4326) not null,
    created_at   timestamptz not null default now()
);

create index external_stores_location_idx on public.external_stores using gist (location);

-- ---------------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------------

create table public.categories (
    id         smallint generated always as identity primary key,
    name       text not null,
    slug       text not null unique,
    icon       text,
    sort_order smallint not null default 0
);

create table public.products (
    id          uuid primary key default gen_random_uuid(),
    store_id    uuid not null references public.stores (id) on delete cascade,
    category_id smallint not null references public.categories (id),
    name        text not null,
    description text,
    brand       text,
    species     public.pet_species[] not null default '{}',
    image_urls  text[] not null default '{}',
    is_active   boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create index products_store_idx on public.products (store_id, category_id);

-- Cada tamanho ou apresentação do produto (ex.: ração X em 1 kg, 3 kg e 15 kg) (RF-30).
create table public.product_variants (
    id          uuid primary key default gen_random_uuid(),
    product_id  uuid not null references public.products (id) on delete cascade,
    label       text not null,
    price_cents integer not null check (price_cents > 0),
    stock       integer not null default 0 check (stock >= 0),
    is_active   boolean not null default true, -- pausar item em falta (RF-86)
    sort_order  smallint not null default 0,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create index product_variants_product_idx on public.product_variants (product_id);

-- ---------------------------------------------------------------------------
-- Pedidos
-- ---------------------------------------------------------------------------

create table public.orders (
    id                 uuid primary key default gen_random_uuid(),
    number             bigint generated always as identity unique, -- número curto para falar com a loja
    customer_id        uuid not null references public.profiles (id),
    store_id           uuid not null references public.stores (id),
    courier_id         uuid references public.profiles (id),
    status             public.order_status not null default 'pending',

    -- Cópias feitas no momento do pedido: mudanças posteriores no perfil, no endereço
    -- ou na loja não alteram um pedido já feito (RN-03).
    customer_name      text not null,
    customer_phone     text,
    courier_name       text,
    street             text not null,
    number_address     text not null,
    complement         text,
    neighborhood       text not null,
    reference          text,
    location           extensions.geography (point, 4326) not null,
    distance_m         integer not null,

    subtotal_cents     integer not null check (subtotal_cents > 0),
    delivery_fee_cents integer not null check (delivery_fee_cents >= 0),
    total_cents        integer not null,
    payment_method     public.payment_method not null,
    change_for_cents   integer check (change_for_cents is null or change_for_cents > 0),
    notes              text,
    status_reason      text, -- motivo de recusa, cancelamento ou não entrega

    created_at         timestamptz not null default now(),
    accepted_at        timestamptz,
    out_for_delivery_at timestamptz,
    finished_at        timestamptz,
    updated_at         timestamptz not null default now(),

    check (total_cents = subtotal_cents + delivery_fee_cents),
    check (change_for_cents is null or payment_method = 'cash')
);

create index orders_customer_idx on public.orders (customer_id, created_at desc);
create index orders_store_idx on public.orders (store_id, status, created_at desc);
create index orders_courier_idx on public.orders (courier_id, status) where courier_id is not null;
create index orders_pending_idx on public.orders (created_at) where status = 'pending';

create table public.order_items (
    id               uuid primary key default gen_random_uuid(),
    order_id         uuid not null references public.orders (id) on delete cascade,
    variant_id       uuid references public.product_variants (id) on delete set null,
    product_name     text not null,
    variant_label    text not null,
    unit_price_cents integer not null check (unit_price_cents > 0),
    quantity         integer not null check (quantity > 0),
    line_total_cents integer generated always as (unit_price_cents * quantity) stored
);

create index order_items_order_idx on public.order_items (order_id);

create table public.order_status_history (
    id          bigint generated always as identity primary key,
    order_id    uuid not null references public.orders (id) on delete cascade,
    from_status public.order_status,
    to_status   public.order_status not null,
    changed_by  uuid references public.profiles (id),
    reason      text,
    created_at  timestamptz not null default now()
);

create index order_status_history_order_idx on public.order_status_history (order_id, created_at);

-- Última posição do entregador, só enquanto o pedido está saindo para entrega (RN-11).
-- Uma linha por pedido, sobrescrita a cada atualização e apagada ao finalizar.
create table public.courier_locations (
    order_id   uuid primary key references public.orders (id) on delete cascade,
    courier_id uuid not null references public.profiles (id),
    location   extensions.geography (point, 4326) not null,
    heading    real, -- direção em graus (0 = norte)
    speed_mps  real,
    updated_at timestamptz not null default now()
);
