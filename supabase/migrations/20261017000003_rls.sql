-- Xodó Pet: Row Level Security (RNF-04).
--
-- Regra geral: com RLS ligado, tudo que não tem política é negado.
-- Pedidos, itens, histórico e localização do entregador NÃO têm política de escrita:
-- só mudam pelas funções place_order, transition_order, assign_courier e update_courier_location,
-- que validam as regras de negócio antes de gravar.

alter table public.profiles             enable row level security;
alter table public.addresses            enable row level security;
alter table public.stores               enable row level security;
alter table public.store_members        enable row level security;
alter table public.external_stores      enable row level security;
alter table public.categories           enable row level security;
alter table public.products             enable row level security;
alter table public.product_variants     enable row level security;
alter table public.orders               enable row level security;
alter table public.order_items          enable row level security;
alter table public.order_status_history enable row level security;
alter table public.courier_locations    enable row level security;

-- ---------------------------------------------------------------------------
-- Perfis e endereços: cada um vê e edita só o próprio
-- ---------------------------------------------------------------------------

create policy "perfil: ler o próprio" on public.profiles
    for select to authenticated
    using (id = (select auth.uid()) or (select private.is_admin()));

-- is_admin é protegido pelo gatilho protect_is_admin.
create policy "perfil: editar o próprio" on public.profiles
    for update to authenticated
    using (id = (select auth.uid()) or (select private.is_admin()))
    with check (id = (select auth.uid()) or (select private.is_admin()));

create policy "endereço: tudo no próprio" on public.addresses
    for all to authenticated
    using (profile_id = (select auth.uid()))
    with check (profile_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- Lojas
-- ---------------------------------------------------------------------------

-- Qualquer um (inclusive sem login) vê lojas ativas; a equipe vê a própria loja mesmo inativa.
create policy "loja: ler ativas" on public.stores
    for select to anon, authenticated
    using (is_active or private.store_role(id) is not null or (select private.is_admin()));

create policy "loja: admin cria" on public.stores
    for insert to authenticated
    with check ((select private.is_admin()));

-- is_active é protegido pelo gatilho protect_is_active.
create policy "loja: equipe edita" on public.stores
    for update to authenticated
    using (private.can_manage_store(id))
    with check (private.can_manage_store(id));

create policy "loja: admin apaga" on public.stores
    for delete to authenticated
    using ((select private.is_admin()));

-- Equipe: cada um vê as próprias participações; dono e atendente veem a equipe da loja.
-- Entrar na equipe é pela função add_store_member; o dono remove.
create policy "equipe: ler" on public.store_members
    for select to authenticated
    using (profile_id = (select auth.uid()) or private.can_manage_store(store_id));

create policy "equipe: dono remove" on public.store_members
    for delete to authenticated
    using ((select private.is_admin()) or private.store_role(store_id) = 'owner');

-- Dono e atendente precisam do nome dos entregadores para atribuir pedidos.
create policy "perfil: equipe vê entregadores da loja" on public.profiles
    for select to authenticated
    using (exists (
        select 1 from public.store_members m
        where m.profile_id = profiles.id and private.can_manage_store(m.store_id)
    ));

create policy "lojas externas: ler" on public.external_stores
    for select to anon, authenticated
    using (true);

create policy "lojas externas: admin escreve" on public.external_stores
    for all to authenticated
    using ((select private.is_admin()))
    with check ((select private.is_admin()));

-- ---------------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------------

create policy "categoria: ler" on public.categories
    for select to anon, authenticated
    using (true);

create policy "categoria: admin escreve" on public.categories
    for all to authenticated
    using ((select private.is_admin()))
    with check ((select private.is_admin()));

-- O público vê produtos ativos de lojas ativas; a equipe vê todo o catálogo da loja (inclusive pausados).
create policy "produto: ler" on public.products
    for select to anon, authenticated
    using (
        (is_active and exists (select 1 from public.stores s where s.id = store_id and s.is_active))
        or private.can_manage_store(store_id)
    );

create policy "produto: equipe escreve" on public.products
    for all to authenticated
    using (private.can_manage_store(store_id))
    with check (private.can_manage_store(store_id));

-- Variantes pausadas continuam visíveis (o app mostra "indisponível", RF-24); o pedido recusa.
create policy "variante: ler" on public.product_variants
    for select to anon, authenticated
    using (exists (select 1 from public.products p where p.id = product_id));

create policy "variante: equipe escreve" on public.product_variants
    for all to authenticated
    using (exists (
        select 1 from public.products p
        where p.id = product_id and private.can_manage_store(p.store_id)
    ))
    with check (exists (
        select 1 from public.products p
        where p.id = product_id and private.can_manage_store(p.store_id)
    ));

-- ---------------------------------------------------------------------------
-- Pedidos (só leitura; a escrita é pelas funções)
-- ---------------------------------------------------------------------------

-- Cliente vê os próprios; dono e atendente veem os da loja; entregador vê os atribuídos a ele.
create policy "pedido: ler" on public.orders
    for select to authenticated
    using (
        customer_id = (select auth.uid())
        or courier_id = (select auth.uid())
        or private.can_manage_store(store_id)
    );

-- As políticas abaixo consultam orders, cuja própria política já filtra o que a pessoa pode ver.
create policy "item: ler" on public.order_items
    for select to authenticated
    using (exists (select 1 from public.orders o where o.id = order_id));

create policy "histórico: ler" on public.order_status_history
    for select to authenticated
    using (exists (select 1 from public.orders o where o.id = order_id));

create policy "localização: ler" on public.courier_locations
    for select to authenticated
    using (exists (select 1 from public.orders o where o.id = order_id));
