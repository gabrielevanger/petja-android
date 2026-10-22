-- PetJá: anda um pedido para a próxima etapa, como se a loja tivesse tocado no botão.
-- Só para desenvolvimento, enquanto o modo lojista (#18) não existe. Rode no SQL Editor do Supabase.
--
-- 1. Troque o número do pedido (o mesmo "#N" que o app mostra).
-- 2. Deixe v_to em branco para seguir o caminho feliz:
--      pending -> accepted -> preparing -> out_for_delivery -> delivered
--    ou escolha uma etapa: accepted, preparing, out_for_delivery, delivered,
--    rejected, canceled ou not_delivered (estas três pedem um motivo).
--
-- Usa a mesma função do app (private.apply_order_transition): confere se a transição é permitida,
-- grava o histórico e devolve o estoque quando o pedido não se concretiza. O app do cliente
-- recebe a mudança pelo Realtime na hora.

do $$
declare
    v_number bigint := 1;
    v_to     text   := '';
    v_reason text   := '';

    v_order public.orders;
    v_next  public.order_status;
    v_actor uuid;
begin
    select * into v_order from public.orders where number = v_number for update;
    if not found then
        raise exception 'Nenhum pedido #%.', v_number;
    end if;

    v_next := coalesce(
        nullif(trim(v_to), '')::public.order_status,
        (case v_order.status
            when 'pending' then 'accepted'
            when 'accepted' then 'preparing'
            when 'preparing' then 'out_for_delivery'
            when 'out_for_delivery' then 'delivered'
        end)::public.order_status
    );
    if v_next is null then
        raise exception 'O pedido #% já terminou (%).', v_number, v_order.status;
    end if;
    if v_next in ('rejected', 'canceled', 'not_delivered') and nullif(trim(v_reason), '') is null then
        raise exception 'Para % informe um motivo em v_reason.', v_next;
    end if;

    -- No histórico, a mudança fica em nome do dono da loja, como seria pelo app.
    select profile_id into v_actor
    from public.store_members
    where store_id = v_order.store_id and role = 'owner'
    limit 1;

    perform private.apply_order_transition(v_order, v_next, v_actor, nullif(trim(v_reason), ''));
    raise notice 'Pedido #%: % -> %.', v_number, v_order.status, v_next;
end;
$$;
