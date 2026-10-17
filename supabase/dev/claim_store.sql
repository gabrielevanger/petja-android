-- Xodó Pet: liga a SUA conta a uma loja fictícia, para testar o modo lojista e o admin.
-- Só para desenvolvimento. Rode no SQL Editor do Supabase depois de se cadastrar no app.
--
-- 1. Troque o e-mail abaixo pelo da sua conta de teste.
-- 2. Escolha a loja (os IDs fixos estão no seed.sql; ...0001 é a Petshop Lavrado, no Centro).

do $$
declare
    v_email    text := 'seu-email-de-teste@exemplo.com';
    v_store_id uuid := '00000000-0000-4000-a000-000000000001';
    v_user_id  uuid;
begin
    select id into v_user_id from auth.users where lower(email) = lower(v_email);
    if v_user_id is null then
        raise exception 'Nenhuma conta com o e-mail %. Cadastre-se no app primeiro.', v_email;
    end if;

    -- Admin: vê e edita tudo (comente esta linha se quiser testar como lojista comum).
    update public.profiles set is_admin = true where id = v_user_id;

    -- Dono da loja escolhida.
    insert into public.store_members (store_id, profile_id, role)
    values (v_store_id, v_user_id, 'owner')
    on conflict (store_id, profile_id) do update set role = 'owner';

    raise notice 'Pronto: % agora é admin e dono da loja %.', v_email, v_store_id;
end;
$$;
