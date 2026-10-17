-- Xodó Pet: recursos específicos do Supabase.
-- Cada bloco confere se o recurso existe, para a migração também rodar num Postgres comum
-- (testes locais) sem quebrar.

-- ---------------------------------------------------------------------------
-- Realtime: status do pedido (RF-60) e posição do entregador (RF-69)
-- ---------------------------------------------------------------------------
-- O app escuta mudanças nessas tabelas com supabase.channel(...).postgresChanges(...).
-- O Realtime respeita as políticas de RLS: cada um só recebe os pedidos que pode ler.
do $$
begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        alter publication supabase_realtime add table public.orders, public.courier_locations;
    end if;
end;
$$;

-- ---------------------------------------------------------------------------
-- Storage: fotos de produtos e logos das lojas (RF-85)
-- ---------------------------------------------------------------------------
-- Bucket público para leitura. Caminho dos arquivos: <store_id>/<nome-do-arquivo>.
-- Só a equipe da loja (ou o admin) envia, troca e apaga arquivos da pasta da loja.
do $$
begin
    if exists (select 1 from pg_namespace where nspname = 'storage') then
        insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
        values ('store-media', 'store-media', true, 2 * 1024 * 1024,
                array['image/jpeg', 'image/png', 'image/webp'])
        on conflict (id) do nothing;

        create policy "fotos: equipe envia" on storage.objects
            for insert to authenticated
            with check (
                bucket_id = 'store-media'
                and private.can_manage_store(((storage.foldername(name))[1])::uuid)
            );

        create policy "fotos: equipe troca" on storage.objects
            for update to authenticated
            using (
                bucket_id = 'store-media'
                and private.can_manage_store(((storage.foldername(name))[1])::uuid)
            );

        create policy "fotos: equipe apaga" on storage.objects
            for delete to authenticated
            using (
                bucket_id = 'store-media'
                and private.can_manage_store(((storage.foldername(name))[1])::uuid)
            );
    end if;
end;
$$;

-- ---------------------------------------------------------------------------
-- pg_cron: cancelamento automático de pedidos sem resposta (RN-06)
-- ---------------------------------------------------------------------------
do $$
begin
    if exists (select 1 from pg_available_extensions where name = 'pg_cron') then
        create extension if not exists pg_cron;
        perform cron.schedule(
            'expire-pending-orders',
            '* * * * *',
            'select private.expire_pending_orders(10)'
        );
    else
        raise notice 'pg_cron indisponível: rode private.expire_pending_orders() manualmente.';
    end if;
end;
$$;
