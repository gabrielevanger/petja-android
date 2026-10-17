-- Xodó Pet: regras de negócio no banco.
--
-- Funções em `public` são chamadas pelo app via RPC (supabase.postgrest.rpc("place_order", ...)).
-- Funções em `private` são auxiliares: o schema não é exposto pela API do Supabase.
--
-- Erros de regra de negócio usam a mensagem como código estável (ex.: 'out_of_delivery_area'),
-- para o app traduzir em texto para o usuário; o `detail` traz a explicação em português.
--
-- Toda função usa `set search_path = ''` e nomes qualificados, como recomenda o Supabase
-- para funções security definer.

create schema if not exists private;
grant usage on schema private to anon, authenticated, service_role;

-- ---------------------------------------------------------------------------
-- Auxiliares
-- ---------------------------------------------------------------------------

create function private.make_point(p_lat double precision, p_lng double precision)
returns extensions.geography
language sql
immutable
set search_path = ''
as $$
    select extensions.st_setsrid(extensions.st_makepoint(p_lng, p_lat), 4326)::extensions.geography
$$;

create function private.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce((select is_admin from public.profiles where id = auth.uid()), false)
$$;

-- Papel do usuário atual na loja, ou null se não for membro.
create function private.store_role(p_store_id uuid)
returns public.store_role
language sql
stable
security definer
set search_path = ''
as $$
    select role from public.store_members
    where store_id = p_store_id and profile_id = auth.uid()
$$;

-- Dono ou atendente: quem opera a loja (pedidos, catálogo, configurações).
create function private.can_manage_store(p_store_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select private.is_admin()
        or coalesce(private.store_role(p_store_id) in ('owner', 'staff'), false)
$$;

-- Taxa de entrega (RN-07): taxa base até included_km, mais o valor por km adicional (arredondado para cima).
create function private.delivery_fee_cents(
    p_base_fee_cents integer,
    p_included_km numeric,
    p_extra_km_fee_cents integer,
    p_distance_m integer
)
returns integer
language sql
immutable
set search_path = ''
as $$
    select p_base_fee_cents
        + (ceil(greatest(0, p_distance_m / 1000.0 - p_included_km)) * p_extra_km_fee_cents)::integer
$$;

-- Linhas do carrinho vindas do app, juntando linhas repetidas da mesma variante.
-- p_items: [{"variant_id": "...", "quantity": 2}, ...]
create function private.cart_lines(p_items jsonb)
returns table (variant_id uuid, quantity integer)
language sql
immutable
set search_path = ''
as $$
    select (item ->> 'variant_id')::uuid, sum((item ->> 'quantity')::integer)::integer
    from jsonb_array_elements(p_items) as item
    group by 1
$$;

-- Transições permitidas pela máquina de estados (REQUISITOS.md, seção 4.1).
create function private.order_transition_allowed(p_from public.order_status, p_to public.order_status)
returns boolean
language sql
immutable
set search_path = ''
as $$
    select case p_from
        when 'pending'          then p_to in ('accepted', 'rejected', 'canceled')
        when 'accepted'         then p_to in ('preparing', 'canceled')
        when 'preparing'        then p_to in ('out_for_delivery', 'canceled')
        when 'out_for_delivery' then p_to in ('delivered', 'not_delivered')
        else false -- delivered, not_delivered, rejected e canceled são finais
    end
$$;

-- Aplica a transição sem checar quem está pedindo; quem chama já checou.
-- Grava o histórico, as datas, devolve o estoque quando o pedido não se concretiza
-- e apaga a localização do entregador quando a entrega termina (RN-11).
create function private.apply_order_transition(
    p_order public.orders,
    p_to public.order_status,
    p_actor uuid,
    p_reason text
)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_order public.orders;
begin
    if not private.order_transition_allowed(p_order.status, p_to) then
        raise exception 'invalid_transition'
            using detail = format('O pedido não pode ir de %s para %s.', p_order.status, p_to);
    end if;

    update public.orders set
        status = p_to,
        status_reason = coalesce(p_reason, status_reason),
        accepted_at = case when p_to = 'accepted' then now() else accepted_at end,
        out_for_delivery_at = case when p_to = 'out_for_delivery' then now() else out_for_delivery_at end,
        finished_at = case
            when p_to in ('delivered', 'not_delivered', 'rejected', 'canceled') then now()
            else finished_at
        end
    where id = p_order.id
    returning * into v_order;

    insert into public.order_status_history (order_id, from_status, to_status, changed_by, reason)
    values (p_order.id, p_order.status, p_to, p_actor, p_reason);

    -- O produto volta para a prateleira quando o pedido não se concretiza.
    if p_to in ('rejected', 'canceled', 'not_delivered') then
        update public.product_variants v
        set stock = v.stock + i.quantity
        from public.order_items i
        where i.order_id = p_order.id and i.variant_id = v.id;
    end if;

    if p_to in ('delivered', 'not_delivered', 'rejected', 'canceled') then
        delete from public.courier_locations where order_id = p_order.id;
    end if;

    return v_order;
end;
$$;

-- ---------------------------------------------------------------------------
-- Gatilhos
-- ---------------------------------------------------------------------------

create function private.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger set_updated_at before update on public.profiles
    for each row execute function private.set_updated_at();
create trigger set_updated_at before update on public.stores
    for each row execute function private.set_updated_at();
create trigger set_updated_at before update on public.products
    for each row execute function private.set_updated_at();
create trigger set_updated_at before update on public.product_variants
    for each row execute function private.set_updated_at();
create trigger set_updated_at before update on public.orders
    for each row execute function private.set_updated_at();

-- Cria o perfil quando alguém se cadastra no Supabase Auth.
-- O app manda o nome em options.data.full_name no signUp; o login com Google manda em "name".
create function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (id, full_name, phone)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name'),
        new.raw_user_meta_data ->> 'phone'
    );
    return new;
end;
$$;

create trigger on_auth_user_created after insert on auth.users
    for each row execute function private.handle_new_user();

-- Só o admin muda profiles.is_admin e stores.is_active. As políticas de RLS deixam o usuário
-- editar o próprio perfil e o lojista editar a própria loja; estes gatilhos fecham essas duas colunas.
-- Chamadas do painel do Supabase e da service_role não passam pelos papéis anon/authenticated.
create function private.protect_profile_is_admin()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.is_admin is distinct from old.is_admin
       and current_user in ('anon', 'authenticated')
       and not private.is_admin() then
        raise exception 'forbidden' using detail = 'Só o admin altera o papel de admin.';
    end if;
    return new;
end;
$$;

create function private.protect_store_is_active()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.is_active is distinct from old.is_active
       and current_user in ('anon', 'authenticated')
       and not private.is_admin() then
        raise exception 'forbidden' using detail = 'Só o admin ativa ou desativa uma loja.';
    end if;
    return new;
end;
$$;

create trigger protect_is_admin before update on public.profiles
    for each row execute function private.protect_profile_is_admin();
create trigger protect_is_active before update on public.stores
    for each row execute function private.protect_store_is_active();

-- Ao marcar um endereço como padrão, desmarca os outros da mesma pessoa.
create function private.single_default_address()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.is_default then
        update public.addresses
        set is_default = false
        where profile_id = new.profile_id and id <> new.id and is_default;
    end if;
    return new;
end;
$$;

create trigger single_default_address before insert or update of is_default on public.addresses
    for each row execute function private.single_default_address();

-- ---------------------------------------------------------------------------
-- RPC: vitrine
-- ---------------------------------------------------------------------------

-- Lojas que entregam no ponto informado (RN-02), abertas primeiro e depois por distância (RF-20).
-- Funciona sem login (RF-01).
create function public.stores_near(p_lat double precision, p_lng double precision)
returns table (
    id                 uuid,
    name               text,
    slug               text,
    description        text,
    logo_url           text,
    neighborhood       text,
    whatsapp           text,
    is_open            boolean,
    min_order_cents    integer,
    estimated_minutes  integer,
    distance_m         integer,
    delivery_fee_cents integer
)
language sql
stable
set search_path = ''
as $$
    with point as (select private.make_point(p_lat, p_lng) as location)
    select
        s.id, s.name, s.slug, s.description, s.logo_url, s.neighborhood, s.whatsapp,
        s.is_open, s.min_order_cents, s.estimated_minutes,
        d.distance_m,
        private.delivery_fee_cents(s.base_fee_cents, s.included_km, s.extra_km_fee_cents, d.distance_m)
    from public.stores s
    cross join point p
    cross join lateral (
        select extensions.st_distance(s.location, p.location)::integer as distance_m
    ) d
    where s.is_active
      and extensions.st_dwithin(s.location, p.location, s.delivery_radius_km * 1000)
    order by s.is_open desc, d.distance_m
$$;

-- Petshops não parceiros mais próximos, para quando nenhuma parceira entrega (RF-15).
create function public.external_stores_near(
    p_lat double precision,
    p_lng double precision,
    p_limit integer default 5,
    p_max_km numeric default 10
)
returns table (
    id           uuid,
    name         text,
    phone        text,
    whatsapp     text,
    street       text,
    neighborhood text,
    lat          double precision,
    lng          double precision,
    distance_m   integer
)
language sql
stable
set search_path = ''
as $$
    with point as (select private.make_point(p_lat, p_lng) as location)
    select
        e.id, e.name, e.phone, e.whatsapp, e.street, e.neighborhood,
        extensions.st_y(e.location::extensions.geometry),
        extensions.st_x(e.location::extensions.geometry),
        extensions.st_distance(e.location, p.location)::integer
    from public.external_stores e
    cross join point p
    where extensions.st_dwithin(e.location, p.location, p_max_km * 1000)
    order by e.location operator(extensions.<->) p.location
    limit least(greatest(p_limit, 1), 20)
$$;

-- ---------------------------------------------------------------------------
-- RPC: pedido
-- ---------------------------------------------------------------------------

-- Cria o pedido validando tudo no servidor: loja ativa e aberta, endereço dentro do raio,
-- itens da mesma loja e disponíveis, estoque, pedido mínimo (RN-01 a RN-10, RF-45).
--
-- p_items: [{"variant_id": "...", "quantity": 2}, ...]
-- p_expected_total_cents: total que o app mostrou ao cliente. Se o preço ou a taxa mudaram
-- desde então, o pedido é recusado com 'price_changed' e o app mostra os valores novos (RF-45).
create function public.place_order(
    p_store_id uuid,
    p_address_id uuid,
    p_items jsonb,
    p_payment_method public.payment_method,
    p_change_for_cents integer default null,
    p_notes text default null,
    p_expected_total_cents integer default null
)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid        uuid := auth.uid();
    v_profile    public.profiles;
    v_address    public.addresses;
    v_store      public.stores;
    v_distance_m integer;
    v_subtotal   integer;
    v_fee        integer;
    v_requested  integer;
    v_found      integer;
    v_order      public.orders;
begin
    if v_uid is null then
        raise exception 'not_authenticated' using detail = 'Entre na sua conta para fazer o pedido.';
    end if;

    select * into v_profile from public.profiles where id = v_uid;

    select * into v_address from public.addresses where id = p_address_id and profile_id = v_uid;
    if not found then
        raise exception 'address_not_found' using detail = 'Endereço não encontrado.';
    end if;

    select * into v_store from public.stores where id = p_store_id and is_active;
    if not found then
        raise exception 'store_not_found' using detail = 'Loja não encontrada.';
    end if;
    if not v_store.is_open then
        raise exception 'store_closed' using detail = 'A loja está fechada agora.';
    end if;

    v_distance_m := extensions.st_distance(v_store.location, v_address.location)::integer;
    if v_distance_m > v_store.delivery_radius_km * 1000 then
        raise exception 'out_of_delivery_area' using detail = 'A loja não entrega neste endereço.';
    end if;

    if jsonb_typeof(p_items) is distinct from 'array' or jsonb_array_length(p_items) = 0 then
        raise exception 'empty_cart' using detail = 'O carrinho está vazio.';
    end if;

    if p_change_for_cents is not null and p_payment_method <> 'cash' then
        raise exception 'invalid_change' using detail = 'Troco só vale para pagamento em dinheiro.';
    end if;

    -- Trava as variantes do carrinho até o fim da transação, para dois pedidos simultâneos
    -- não venderem o mesmo estoque.
    if exists (select 1 from private.cart_lines(p_items) where quantity is null or quantity <= 0) then
        raise exception 'invalid_quantity' using detail = 'Quantidade inválida no carrinho.';
    end if;

    select count(*) into v_requested from private.cart_lines(p_items);

    select count(*) into v_found
    from (
        select v.id
        from public.product_variants v
        join public.products p on p.id = v.product_id
        join private.cart_lines(p_items) c on c.variant_id = v.id
        where p.store_id = p_store_id and p.is_active and v.is_active
        for update of v
    ) locked;

    if v_found <> v_requested then
        raise exception 'item_unavailable'
            using detail = 'Algum item não está mais disponível nesta loja.';
    end if;

    if exists (
        select 1 from private.cart_lines(p_items) c
        join public.product_variants v on v.id = c.variant_id
        where v.stock < c.quantity
    ) then
        raise exception 'insufficient_stock'
            using detail = 'A loja não tem a quantidade pedida de algum item.';
    end if;

    select sum(v.price_cents * c.quantity)::integer into v_subtotal
    from private.cart_lines(p_items) c
    join public.product_variants v on v.id = c.variant_id;

    if v_subtotal < v_store.min_order_cents then
        raise exception 'below_min_order'
            using detail = format('O pedido mínimo desta loja é de R$ %s.',
                to_char(v_store.min_order_cents / 100.0, 'FM999G990D00'));
    end if;

    v_fee := private.delivery_fee_cents(
        v_store.base_fee_cents, v_store.included_km, v_store.extra_km_fee_cents, v_distance_m);

    if p_expected_total_cents is not null and p_expected_total_cents <> v_subtotal + v_fee then
        raise exception 'price_changed'
            using detail = 'Os preços ou a taxa de entrega mudaram. Confira o carrinho de novo.';
    end if;

    insert into public.orders (
        customer_id, store_id, customer_name, customer_phone,
        street, number_address, complement, neighborhood, reference, location, distance_m,
        subtotal_cents, delivery_fee_cents, total_cents,
        payment_method, change_for_cents, notes
    ) values (
        v_uid, p_store_id, coalesce(v_profile.full_name, 'Cliente'), v_profile.phone,
        v_address.street, v_address.number, v_address.complement, v_address.neighborhood,
        v_address.reference, v_address.location, v_distance_m,
        v_subtotal, v_fee, v_subtotal + v_fee,
        p_payment_method, p_change_for_cents, nullif(trim(p_notes), '')
    )
    returning * into v_order;

    insert into public.order_items (order_id, variant_id, product_name, variant_label, unit_price_cents, quantity)
    select v_order.id, v.id, p.name, v.label, v.price_cents, c.quantity
    from private.cart_lines(p_items) c
    join public.product_variants v on v.id = c.variant_id
    join public.products p on p.id = v.product_id;

    update public.product_variants v
    set stock = v.stock - c.quantity
    from private.cart_lines(p_items) c
    where c.variant_id = v.id;

    insert into public.order_status_history (order_id, from_status, to_status, changed_by)
    values (v_order.id, null, 'pending', v_uid);

    return v_order;
end;
$$;

-- Muda o status do pedido, conferindo a máquina de estados e quem pode fazer cada mudança:
--   cliente:             pending -> canceled (RN-05)
--   dono/atendente:      qualquer transição válida
--   entregador do pedido: preparing -> out_for_delivery, out_for_delivery -> delivered | not_delivered
--   admin:               qualquer transição válida
-- Recusa, não entrega e cancelamento pela loja exigem motivo.
create function public.transition_order(
    p_order_id uuid,
    p_to public.order_status,
    p_reason text default null
)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid     uuid := auth.uid();
    v_order   public.orders;
    v_reason  text := nullif(trim(p_reason), '');
    v_allowed boolean;
begin
    if v_uid is null then
        raise exception 'not_authenticated' using detail = 'Entre na sua conta.';
    end if;

    select * into v_order from public.orders where id = p_order_id for update;
    if not found then
        raise exception 'order_not_found' using detail = 'Pedido não encontrado.';
    end if;

    v_allowed := case
        when private.can_manage_store(v_order.store_id) then true
        when v_order.courier_id = v_uid then
            (v_order.status, p_to) in (
                ('preparing'::public.order_status, 'out_for_delivery'::public.order_status),
                ('out_for_delivery', 'delivered'),
                ('out_for_delivery', 'not_delivered'))
        when v_order.customer_id = v_uid then
            v_order.status = 'pending' and p_to = 'canceled'
        else false
    end;

    if not v_allowed then
        raise exception 'forbidden' using detail = 'Você não pode fazer essa mudança neste pedido.';
    end if;

    if v_order.customer_id = v_uid and p_to = 'canceled' and not private.can_manage_store(v_order.store_id) then
        v_reason := coalesce(v_reason, 'Cancelado pelo cliente');
    elsif p_to in ('rejected', 'not_delivered', 'canceled') and v_reason is null then
        raise exception 'reason_required' using detail = 'Informe o motivo.';
    end if;

    return private.apply_order_transition(v_order, p_to, v_uid, v_reason);
end;
$$;

-- Atribui um entregador da loja ao pedido (RF-91).
create function public.assign_courier(p_order_id uuid, p_courier_id uuid)
returns public.orders
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_order public.orders;
    v_name  text;
begin
    select * into v_order from public.orders where id = p_order_id for update;
    if not found then
        raise exception 'order_not_found' using detail = 'Pedido não encontrado.';
    end if;
    if not private.can_manage_store(v_order.store_id) then
        raise exception 'forbidden' using detail = 'Só a loja atribui o entregador.';
    end if;
    if v_order.status not in ('accepted', 'preparing') then
        raise exception 'invalid_transition'
            using detail = 'O entregador é atribuído depois de aceitar e antes de sair para entrega.';
    end if;

    select coalesce(p.full_name, 'Entregador') into v_name
    from public.store_members m
    join public.profiles p on p.id = m.profile_id
    where m.store_id = v_order.store_id and m.profile_id = p_courier_id and m.role = 'courier';
    if not found then
        raise exception 'courier_not_found' using detail = 'Essa pessoa não é entregador desta loja.';
    end if;

    update public.orders
    set courier_id = p_courier_id, courier_name = v_name
    where id = p_order_id
    returning * into v_order;

    return v_order;
end;
$$;

-- Posição do entregador durante a entrega (RF-113, RF-69). O app do entregador chama a cada
-- 5 a 10 s. O app do cliente escuta mudanças em courier_locations pelo Supabase Realtime.
create function public.update_courier_location(
    p_order_id uuid,
    p_lat double precision,
    p_lng double precision,
    p_heading real default null,
    p_speed_mps real default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
begin
    if not exists (
        select 1 from public.orders
        where id = p_order_id and courier_id = v_uid and status = 'out_for_delivery'
    ) then
        raise exception 'forbidden'
            using detail = 'Só o entregador do pedido envia a posição, e só durante a entrega.';
    end if;

    insert into public.courier_locations (order_id, courier_id, location, heading, speed_mps, updated_at)
    values (p_order_id, v_uid, private.make_point(p_lat, p_lng), p_heading, p_speed_mps, now())
    on conflict (order_id) do update set
        location = excluded.location,
        heading = excluded.heading,
        speed_mps = excluded.speed_mps,
        updated_at = excluded.updated_at;
end;
$$;

-- ---------------------------------------------------------------------------
-- RPC: equipe da loja
-- ---------------------------------------------------------------------------

-- Adiciona alguém já cadastrado no app à equipe da loja, pelo e-mail (RF-110).
-- Só o dono (ou o admin) gerencia a equipe.
create function public.add_store_member(p_store_id uuid, p_email text, p_role public.store_role)
returns public.store_members
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid;
    v_member  public.store_members;
begin
    -- coalesce: quem não é da equipe tem papel null, e `not null` não entraria no if.
    if not (private.is_admin() or coalesce(private.store_role(p_store_id) = 'owner', false)) then
        raise exception 'forbidden' using detail = 'Só o dono da loja gerencia a equipe.';
    end if;

    select id into v_user_id from auth.users where lower(email) = lower(trim(p_email));
    if not found then
        raise exception 'user_not_found'
            using detail = 'Ninguém com esse e-mail tem conta no app. Peça para a pessoa se cadastrar primeiro.';
    end if;

    insert into public.store_members (store_id, profile_id, role)
    values (p_store_id, v_user_id, p_role)
    on conflict (store_id, profile_id) do update set role = excluded.role
    returning * into v_member;

    return v_member;
end;
$$;

-- ---------------------------------------------------------------------------
-- Rotina: cancelamento automático (RN-06)
-- ---------------------------------------------------------------------------

-- Cancela pedidos pendentes há mais de p_minutes sem resposta da loja.
-- Agendada a cada minuto pelo pg_cron (migração 4). Devolve quantos pedidos cancelou.
create function private.expire_pending_orders(p_minutes integer default 10)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_order public.orders;
    v_count integer := 0;
begin
    for v_order in
        select * from public.orders
        where status = 'pending' and created_at < now() - make_interval(mins => p_minutes)
        for update skip locked
    loop
        perform private.apply_order_transition(
            v_order, 'canceled', null, 'A loja não respondeu a tempo');
        v_count := v_count + 1;
    end loop;
    return v_count;
end;
$$;

-- ---------------------------------------------------------------------------
-- Permissões das funções
-- ---------------------------------------------------------------------------

-- Por padrão o Postgres libera EXECUTE para todos. Fechamos tudo e abrimos só o necessário.
revoke execute on all functions in schema private from public, anon, authenticated;
revoke execute on function
    public.place_order(uuid, uuid, jsonb, public.payment_method, integer, text, integer),
    public.transition_order(uuid, public.order_status, text),
    public.assign_courier(uuid, uuid),
    public.update_courier_location(uuid, double precision, double precision, real, real),
    public.add_store_member(uuid, text, public.store_role)
from public, anon;

-- Usadas dentro das políticas de RLS e das funções de vitrine, que rodam com o papel de quem chama.
grant execute on function
    private.make_point(double precision, double precision),
    private.is_admin(),
    private.store_role(uuid),
    private.can_manage_store(uuid),
    private.delivery_fee_cents(integer, numeric, integer, integer)
to anon, authenticated;

grant execute on function
    public.place_order(uuid, uuid, jsonb, public.payment_method, integer, text, integer),
    public.transition_order(uuid, public.order_status, text),
    public.assign_courier(uuid, uuid),
    public.update_courier_location(uuid, double precision, double precision, real, real),
    public.add_store_member(uuid, text, public.store_role)
to authenticated;

grant execute on function
    public.stores_near(double precision, double precision),
    public.external_stores_near(double precision, double precision, integer, numeric)
to anon, authenticated;
