-- PetJá: endurece o banco depois da revisão de segurança de 28/10/2026.
-- Limites de tamanho e de quantidade, coordenadas válidas, tetos por conta, a equipe da loja por
-- uma função que só devolve o necessário e a leitura de variantes escrita de forma explícita.

-- ---------------------------------------------------------------------------
-- Coordenadas
-- ---------------------------------------------------------------------------

-- Fora do mundo, um erro claro em vez do erro do PostGIS. Vale para endereços, vitrine e entregador.
create or replace function private.make_point(p_lat double precision, p_lng double precision)
returns extensions.geography
language plpgsql
immutable
set search_path = ''
as $$
begin
    if p_lat is null or p_lng is null or p_lat not between -90 and 90 or p_lng not between -180 and 180 then
        raise exception 'invalid_location' using detail = 'Coordenadas inválidas.';
    end if;
    return extensions.st_setsrid(extensions.st_makepoint(p_lng, p_lat), 4326)::extensions.geography;
end
$$;

-- ---------------------------------------------------------------------------
-- Tamanho dos textos
-- ---------------------------------------------------------------------------
-- Ficam nas tabelas, não só nas funções: a RLS deixa o cliente gravar endereço e perfil direto.
-- Os limites são folgados para o uso real (o maior nome de produto do seed tem 48 caracteres).

alter table public.profiles
    add constraint profiles_text_length check (
        char_length(full_name) <= 120
        and char_length(phone) <= 30
    );

alter table public.addresses
    add constraint addresses_text_length check (
        char_length(label) <= 40
        and char_length(street) <= 150
        and char_length(number) <= 20
        and char_length(complement) <= 100
        and char_length(neighborhood) <= 80
        and char_length(reference) <= 150
    );

alter table public.stores
    add constraint stores_text_length check (
        char_length(name) <= 120
        and char_length(slug) <= 120
        and char_length(description) <= 1000
        and char_length(logo_url) <= 500
        and char_length(phone) <= 30
        and char_length(whatsapp) <= 30
        and char_length(street) <= 150
        and char_length(number) <= 20
        and char_length(neighborhood) <= 80
    );

alter table public.products
    add constraint products_text_length check (
        char_length(name) <= 150
        and char_length(description) <= 2000
        and char_length(brand) <= 80
        and cardinality(image_urls) <= 10
    );

alter table public.product_variants
    add constraint product_variants_label_length check (char_length(label) <= 60);

-- O pedido copia nome e endereço de tabelas que já têm limite; aqui só o que o pedido traz de novo.
alter table public.orders
    add constraint orders_text_length check (
        char_length(notes) <= 500
        and char_length(status_reason) <= 300
    );

alter table public.order_status_history
    add constraint order_status_history_reason_length check (char_length(reason) <= 300);

-- ---------------------------------------------------------------------------
-- Quantidades e tetos por conta
-- ---------------------------------------------------------------------------

-- Ninguém compra 100 sacos de ração de uma vez pelo app; acima disso é erro ou abuso.
alter table public.order_items
    add constraint order_items_quantity_max check (quantity <= 99);

create function private.limit_addresses()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select count(*) from public.addresses where profile_id = new.profile_id) >= 20 then
        raise exception 'too_many_addresses' using detail = 'Cada conta guarda até 20 endereços.';
    end if;
    return new;
end
$$;

create trigger addresses_limit
    before insert on public.addresses
    for each row execute function private.limit_addresses();

-- Sem limite de requisições na API do plano grátis, o teto evita encher uma loja de pedidos falsos.
create function private.limit_pending_orders()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (
        select count(*) from public.orders
        where customer_id = new.customer_id and status = 'pending'
    ) >= 5 then
        raise exception 'too_many_pending_orders'
            using detail = 'Você já tem pedidos esperando a loja aceitar. Aguarde a resposta antes de pedir de novo.';
    end if;
    return new;
end
$$;

create trigger orders_pending_limit
    before insert on public.orders
    for each row execute function private.limit_pending_orders();

revoke execute on function private.limit_addresses(), private.limit_pending_orders() from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- Equipe da loja
-- ---------------------------------------------------------------------------
-- Antes, dono e atendente liam o perfil inteiro dos membros da loja (telefone e is_admin incluídos).
-- Agora a equipe vem desta função, só com o que a tela de equipe e a escolha de entregador precisam.

drop policy "perfil: equipe vê entregadores da loja" on public.profiles;

create function public.store_team(p_store_id uuid)
returns table (profile_id uuid, full_name text, role public.store_role)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if not private.can_manage_store(p_store_id) then
        raise exception 'forbidden' using detail = 'Só a equipe da loja vê os membros.';
    end if;
    return query
        select m.profile_id, p.full_name, m.role
        from public.store_members m
        join public.profiles p on p.id = m.profile_id
        where m.store_id = p_store_id
        order by m.role, p.full_name;
end
$$;

revoke execute on function public.store_team(uuid) from public, anon;
grant execute on function public.store_team(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- Variantes
-- ---------------------------------------------------------------------------
-- A regra antiga já escondia variantes de produto pausado (a RLS de produtos vale dentro do exists),
-- mas só de forma implícita. Agora a condição está escrita aqui, igual à de produtos.

drop policy "variante: ler" on public.product_variants;

create policy "variante: ler" on public.product_variants
    for select to anon, authenticated
    using (exists (
        select 1
        from public.products p
        join public.stores s on s.id = p.store_id
        where p.id = product_id
            and ((p.is_active and s.is_active) or private.can_manage_store(p.store_id))
    ));
