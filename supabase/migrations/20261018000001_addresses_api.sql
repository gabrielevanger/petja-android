-- Xodó Pet: endereços para o checkout (RF-10, RF-50).
--
-- O PostgREST devolve geography num formato binário do PostGIS; estas funções entregam latitude e
-- longitude prontas e gravam o ponto a partir delas. Rodam com a permissão de quem chama
-- (security invoker): a RLS de addresses continua decidindo o que cada um vê e grava.

-- Endereços do usuário logado, o padrão primeiro e depois os mais novos.
create function public.my_addresses()
returns table (
    id           uuid,
    label        text,
    street       text,
    number       text,
    complement   text,
    neighborhood text,
    reference    text,
    lat          double precision,
    lng          double precision,
    is_default   boolean
)
language sql
stable
set search_path = ''
as $$
    select
        a.id, a.label, a.street, a.number, a.complement, a.neighborhood, a.reference,
        extensions.st_y(a.location::extensions.geometry),
        extensions.st_x(a.location::extensions.geometry),
        a.is_default
    from public.addresses a
    where a.profile_id = auth.uid()
    order by a.is_default desc, a.created_at desc
$$;

-- Cadastra um endereço do usuário logado e devolve no mesmo formato de my_addresses().
create function public.add_address(
    p_street       text,
    p_number       text,
    p_neighborhood text,
    p_lat          double precision,
    p_lng          double precision,
    p_complement   text default null,
    p_reference    text default null,
    p_label        text default null,
    p_is_default   boolean default true
)
returns table (
    id           uuid,
    label        text,
    street       text,
    number       text,
    complement   text,
    neighborhood text,
    reference    text,
    lat          double precision,
    lng          double precision,
    is_default   boolean
)
language sql
set search_path = ''
as $$
    insert into public.addresses (profile_id, label, street, number, complement, neighborhood, reference, location, is_default)
    values (
        auth.uid(),
        nullif(trim(p_label), ''),
        trim(p_street),
        trim(p_number),
        nullif(trim(p_complement), ''),
        trim(p_neighborhood),
        nullif(trim(p_reference), ''),
        private.make_point(p_lat, p_lng),
        p_is_default
    )
    returning
        addresses.id, addresses.label, addresses.street, addresses.number, addresses.complement,
        addresses.neighborhood, addresses.reference,
        extensions.st_y(addresses.location::extensions.geometry),
        extensions.st_x(addresses.location::extensions.geometry),
        addresses.is_default
$$;

revoke execute on function
    public.my_addresses(),
    public.add_address(text, text, text, double precision, double precision, text, text, text, boolean)
from public, anon;

grant execute on function
    public.my_addresses(),
    public.add_address(text, text, text, double precision, double precision, text, text, text, boolean)
to authenticated;
