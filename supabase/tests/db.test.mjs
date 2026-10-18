import { PGlite } from '@electric-sql/pglite'
import { postgis } from '@electric-sql/pglite-postgis'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'

// Roda as migrações e o seed num Postgres com PostGIS em memória (PGlite) e testa regras e RLS.
// Uso: npm install && npm test
const root = process.argv[2] ?? new URL('..', import.meta.url).pathname
const db = await PGlite.create({ extensions: { postgis } })

// Stub of what Supabase provides: auth schema, roles and default grants.
await db.exec(`
  create schema extensions;
  create schema auth;
  create table auth.users (id uuid primary key, email text, raw_user_meta_data jsonb default '{}');
  create function auth.uid() returns uuid language sql stable as
    $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
  create role anon nologin; create role authenticated nologin; create role service_role nologin bypassrls;
  grant usage on schema public, extensions, auth to anon, authenticated, service_role;
  grant execute on function auth.uid() to anon, authenticated;
  alter default privileges in schema public grant all on tables to anon, authenticated, service_role;
  alter default privileges in schema public grant all on sequences to anon, authenticated, service_role;
`)

const migDir = join(root, 'migrations')
for (const f of readdirSync(migDir).sort()) {
  await db.exec(readFileSync(join(migDir, f), 'utf8'))
  console.log('migration ok:', f)
}
await db.exec(`grant usage on all sequences in schema public to anon, authenticated;
  grant execute on all functions in schema extensions to anon, authenticated;`)
await db.exec(readFileSync(join(root, 'seed.sql'), 'utf8'))
console.log('seed ok')

let failures = 0
const check = (name, cond, extra = '') => {
  console.log(`${cond ? 'PASS' : 'FAIL'} ${name}${extra ? ' :: ' + extra : ''}`)
  if (!cond) failures++
}

async function as(uid, sql, params = []) {
  await db.exec(`reset role; select set_config('request.jwt.claim.sub', '${uid ?? ''}', false); set role ${uid ? 'authenticated' : 'anon'};`)
  try {
    return (await db.query(sql, params)).rows
  } finally {
    await db.exec('reset role')
  }
}
async function expectError(name, uid, sql, params, code) {
  try {
    await as(uid, sql, params)
    check(name, false, 'no error')
  } catch (e) {
    check(name, e.message.includes(code), e.message)
  }
}
const q = async (sql, p) => (await db.query(sql, p)).rows

// --- Seed sanity
const counts = (await q(`select (select count(*) from stores) s, (select count(*) from products) p,
  (select count(*) from product_variants) v, (select count(*) from product_variants where stock = 0) z,
  (select min(price_cents) from product_variants) minp`))[0]
console.log('seed counts', counts)
check('seed has products and some zero stock', counts.p > 80 && counts.z > 0)

// --- Vitrine (anon)
const CENTRO = [2.8200, -60.6720], SATELITE = [2.8600, -60.7600], BURITIS = [2.8230, -60.7150]
let near = await as(null, 'select name, is_open, distance_m, delivery_fee_cents from stores_near($1,$2)', CENTRO)
console.log('Centro:', near)
check('anon sees stores near Centro', near.length >= 3)
check('inactive store hidden', !near.some(r => r.name === 'Pet Center Paraviana'))
near = await as(null, 'select name, is_open from stores_near($1,$2)', BURITIS)
console.log('Buritis:', near)
check('closed store listed last', near.length > 0 && near.findIndex(r => !r.is_open) !== 0)
near = await as(null, 'select name from stores_near($1,$2)', SATELITE)
check('no partner in Cidade Satélite', near.length === 0)
const ext = await as(null, 'select name, distance_m from external_stores_near($1,$2)', SATELITE)
console.log('external:', ext)
check('external stores shown for Satélite', ext.length > 0 && ext[0].name === 'Agropet Satélite')
check('anon sees products', (await as(null, 'select count(*)::int c from products'))[0].c > 0)
check('anon sees no products of inactive store',
  (await as(null, `select count(*)::int c from products where store_id='00000000-0000-4000-a000-000000000006'`))[0].c === 0)

// --- Users
const U = {
  ana: '10000000-0000-4000-a000-000000000001', bia: '10000000-0000-4000-a000-000000000002',
  dono: '10000000-0000-4000-a000-000000000003', moto: '10000000-0000-4000-a000-000000000004',
}
for (const [n, id] of Object.entries(U)) {
  await q(`insert into auth.users (id, email, raw_user_meta_data) values ($1, $2, $3)`,
    [id, `${n}@teste.local`, JSON.stringify({ full_name: n.toUpperCase() })])
}
check('profiles created by trigger', (await q('select count(*)::int c from profiles'))[0].c === 4)
const S1 = '00000000-0000-4000-a000-000000000001'
await q(`insert into store_members values ($1,$2,'owner')`, [S1, U.dono])

// --- Privilege escalation
await expectError('user cannot make self admin', U.ana, `update profiles set is_admin = true where id = $1`, [U.ana], 'forbidden')
const act = await as(U.dono, `update stores set is_active = false where id = $1 returning id`, [S1]).catch(e => e.message)
check('owner cannot deactivate own store', String(act).includes('forbidden'), String(act))
const other = await as(U.dono, `update stores set is_open = false where id = '00000000-0000-4000-a000-000000000002' returning id`)
check('owner cannot edit other store', other.length === 0)
check('owner can open/close own store', (await as(U.dono, `update stores set is_open = true where id = $1 returning id`, [S1])).length === 1)

// --- Address
const [addr] = await as(U.ana, `insert into addresses (profile_id, street, number, neighborhood, reference, location, is_default)
  values ($1, 'Rua Teste', '10', 'Centro', 'Perto da praça', private.make_point($2,$3), true) returning id`, [U.ana, ...CENTRO])
const [addrFar] = await as(U.ana, `insert into addresses (profile_id, street, number, neighborhood, location, is_default)
  values ($1, 'Rua Longe', '99', 'Cidade Satélite', private.make_point($2,$3), true) returning id`, [U.ana, ...SATELITE])
check('single default address', (await q(`select count(*)::int c from addresses where profile_id=$1 and is_default`, [U.ana]))[0].c === 1)
check('bia cannot see ana addresses', (await as(U.bia, 'select * from addresses')).length === 0)
await expectError('cannot insert address for someone else', U.bia,
  `insert into addresses (profile_id, street, number, neighborhood, location) values ($1,'x','1','x',private.make_point(2.82,-60.67))`, [U.ana], 'row-level security')

// --- Place order
const variants = await q(`select v.id, v.price_cents, v.stock from product_variants v join products p on p.id=v.product_id
  where p.store_id=$1 and v.stock >= 5 order by v.price_cents desc limit 2`, [S1])
const items = JSON.stringify([{ variant_id: variants[0].id, quantity: 1 }, { variant_id: variants[1].id, quantity: 2 }, { variant_id: variants[1].id, quantity: 1 }])
const expectedSub = variants[0].price_cents + variants[1].price_cents * 3
const PO = 'select * from place_order($1,$2,$3::jsonb,$4::payment_method,$5,$6,$7)'

await expectError('anon cannot order', null, PO, [S1, addr.id, items, 'cash', null, null, null], 'permission denied')
await expectError('out of delivery area', U.ana, PO, [S1, addrFar.id, items, 'cash', null, null, null], 'out_of_delivery_area')
await expectError('address of someone else', U.bia, PO, [S1, addr.id, items, 'cash', null, null, null], 'address_not_found')
await expectError('price changed', U.ana, PO, [S1, addr.id, items, 'cash', null, null, 1], 'price_changed')
const cheap = await q(`select v.id from product_variants v join products p on p.id=v.product_id where p.store_id=$1 order by price_cents limit 1`, [S1])
await expectError('below min order', U.ana, PO, [S1, addr.id, JSON.stringify([{ variant_id: cheap[0].id, quantity: 1 }]), 'cash', null, null, null], 'below_min_order')
const otherStoreVar = await q(`select v.id from product_variants v join products p on p.id=v.product_id where p.store_id<>$1 limit 1`, [S1])
await expectError('item from another store', U.ana, PO, [S1, addr.id, JSON.stringify([{ variant_id: otherStoreVar[0].id, quantity: 1 }]), 'cash', null, null, null], 'item_unavailable')
await expectError('too much quantity', U.ana, PO, [S1, addr.id, JSON.stringify([{ variant_id: variants[0].id, quantity: 999 }]), 'cash', null, null, null], 'insufficient_stock')
await expectError('change only for cash', U.ana, PO, [S1, addr.id, items, 'pix_on_delivery', 50000, null, null], 'invalid_change')
await expectError('closed store', U.ana, PO, ['00000000-0000-4000-a000-000000000005', addr.id, items, 'cash', null, null, null], 'store_closed')

const fee = (await q(`select delivery_fee_cents from stores_near($1,$2) where id=$3`, [...CENTRO, S1]))[0].delivery_fee_cents
const [order] = await as(U.ana, PO, [S1, addr.id, items, 'cash', 100000, ' Tocar a campainha ', expectedSub + fee])
check('order placed', order.status === 'pending' && order.subtotal_cents === expectedSub && order.total_cents === expectedSub + fee,
  JSON.stringify({ sub: order.subtotal_cents, fee: order.delivery_fee_cents, n: order.number, notes: order.notes }))
check('duplicate lines merged', (await q('select count(*)::int c from order_items where order_id=$1', [order.id]))[0].c === 2)
const stockAfter = (await q('select stock from product_variants where id=$1', [variants[1].id]))[0].stock
check('stock decremented', stockAfter === variants[1].stock - 3)

// --- Visibility and direct writes
check('ana sees her order', (await as(U.ana, 'select id from orders')).length === 1)
check('bia sees nothing', (await as(U.bia, 'select id from orders')).length === 0)
check('anon sees nothing', (await as(null, 'select id from orders').catch(() => [])).length === 0)
check('owner sees store order', (await as(U.dono, 'select id from orders')).length === 1)
check('owner sees items', (await as(U.dono, 'select id from order_items')).length === 2)
check('direct update blocked', (await as(U.ana, `update orders set total_cents = 1 where id=$1 returning id`, [order.id])).length === 0)
await expectError('direct insert blocked', U.ana, `insert into orders (customer_id, store_id, customer_name, street, number_address, neighborhood, location, distance_m, subtotal_cents, delivery_fee_cents, total_cents, payment_method)
  values ($1,$2,'x','x','1','x',private.make_point(2.82,-60.67),1,1,0,1,'cash')`, [U.ana, S1], 'row-level security')

// --- Workflow
const T = 'select * from transition_order($1,$2::order_status,$3)'
await expectError('customer cannot accept', U.ana, T, [order.id, 'accepted', null], 'forbidden')
await expectError('cannot skip states', U.dono, T, [order.id, 'out_for_delivery', null], 'invalid_transition')
await expectError('reject needs reason', U.dono, T, [order.id, 'rejected', null], 'reason_required')
check('owner accepts', (await as(U.dono, T, [order.id, 'accepted', null]))[0].accepted_at !== null)
await expectError('customer cannot cancel after accepted', U.ana, T, [order.id, 'canceled', null], 'forbidden')

await expectError('customer cannot add member', U.ana, `select * from add_store_member($1,$2,'courier')`, [S1, 'moto@teste.local'], 'forbidden')
await expectError('unknown email', U.dono, `select * from add_store_member($1,$2,'courier')`, [S1, 'ninguem@teste.local'], 'user_not_found')
await as(U.dono, `select * from add_store_member($1,$2,'courier')`, [S1, 'MOTO@teste.local'])
check('owner sees courier profile', (await as(U.dono, 'select full_name from profiles where id=$1', [U.moto])).length === 1)
await expectError('cannot assign non courier', U.dono, 'select * from assign_courier($1,$2)', [order.id, U.bia], 'courier_not_found')
const [assigned] = await as(U.dono, 'select * from assign_courier($1,$2)', [order.id, U.moto])
check('courier assigned', assigned.courier_name === 'MOTO')
check('courier sees order', (await as(U.moto, 'select id from orders')).length === 1)
check('courier does not see catalog admin', (await as(U.moto, `update stores set is_open=false where id=$1 returning id`, [S1])).length === 0)
await expectError('courier cannot accept/prepare', U.moto, T, [order.id, 'preparing', null], 'forbidden')
await as(U.dono, T, [order.id, 'preparing', null])
await expectError('location before delivery start', U.moto, 'select update_courier_location($1,$2,$3)', [order.id, 2.82, -60.68], 'forbidden')
check('courier starts delivery', (await as(U.moto, T, [order.id, 'out_for_delivery', null]))[0].out_for_delivery_at !== null)
await as(U.moto, 'select update_courier_location($1,$2,$3,$4,$5)', [order.id, 2.8230, -60.6735, 90, 8])
await as(U.moto, 'select update_courier_location($1,$2,$3,$4,$5)', [order.id, 2.8215, -60.6728, 120, 7])
await expectError('other user cannot send location', U.bia, 'select update_courier_location($1,$2,$3)', [order.id, 2.82, -60.68], 'forbidden')
const loc = await as(U.ana, 'select extensions.st_y(location::extensions.geometry) lat from courier_locations')
check('customer sees courier location', loc.length === 1 && Math.abs(loc[0].lat - 2.8215) < 1e-6)
check('bia cannot see location', (await as(U.bia, 'select * from courier_locations')).length === 0)
await as(U.moto, T, [order.id, 'delivered', null])
check('location deleted after delivery', (await q('select count(*)::int c from courier_locations'))[0].c === 0)
await expectError('final state is final', U.dono, T, [order.id, 'canceled', 'teste'], 'invalid_transition')
const hist = await as(U.ana, 'select from_status, to_status from order_status_history where order_id=$1 order by id', [order.id])
check('history recorded', hist.map(h => h.to_status).join('>') === 'pending>accepted>preparing>out_for_delivery>delivered', hist.map(h => h.to_status).join('>'))
check('stock not restored on delivery', (await q('select stock from product_variants where id=$1', [variants[1].id]))[0].stock === stockAfter)

// --- Customer cancel restores stock
const [o2] = await as(U.ana, PO, [S1, addr.id, items, 'pix_on_delivery', null, null, null])
const [c2] = await as(U.ana, T, [o2.id, 'canceled', null])
check('customer cancels pending', c2.status === 'canceled' && c2.status_reason === 'Cancelado pelo cliente')
check('stock restored on cancel', (await q('select stock from product_variants where id=$1', [variants[1].id]))[0].stock === stockAfter)

// --- Auto-expire (RN-06)
const [o3] = await as(U.ana, PO, [S1, addr.id, items, 'cash', null, null, null])
await q(`update orders set created_at = now() - interval '11 minutes' where id=$1`, [o3.id])
const [o4] = await as(U.ana, PO, [S1, addr.id, items, 'cash', null, null, null])
const expired = (await q('select private.expire_pending_orders(10) n'))[0].n
const st = await q('select id, status, status_reason from orders where id in ($1,$2)', [o3.id, o4.id])
check('expire cancels only old pending', expired === 1 && st.find(r => r.id === o3.id).status === 'canceled' && st.find(r => r.id === o4.id).status === 'pending')
await expectError('expire not callable by users', U.ana, 'select private.expire_pending_orders(0)', [], 'permission denied')

// --- Addresses API (checkout)
const added = await as(U.bia, `select * from add_address('Rua Nova', '12', 'Caçari', 2.833, -60.676, null, 'Portão azul', 'Casa')`)
check('add_address returns the point', added.length === 1 && Math.abs(added[0].lat - 2.833) < 1e-9 && Math.abs(added[0].lng + 60.676) < 1e-9 && added[0].reference === 'Portão azul')
const mine = await as(U.bia, 'select * from my_addresses()')
check('my_addresses lists only own addresses', mine.length === 1 && mine[0].id === added[0].id)
check('my_addresses of another user is empty for them', (await as(U.dono, 'select * from my_addresses()')).length === 0)
const [second] = await as(U.bia, `select * from add_address('Rua Dois', '5', 'Centro', 2.82, -60.672)`)
const listed = await as(U.bia, 'select id, is_default from my_addresses()')
check('new default address comes first and is the only default', listed[0].id === second.id && listed.filter(a => a.is_default).length === 1)
await expectError('anon cannot add address', null, `select * from add_address('x','1','y',2.82,-60.67)`, [], 'permission denied')
const [orderFromNew] = await as(U.bia, PO, [S1, second.id, items, 'cash', null, null, null])
check('order with an address from add_address', orderFromNew.status === 'pending' && orderFromNew.street === 'Rua Dois')

// --- Catalog management
const [np] = await as(U.dono, `insert into products (store_id, category_id, name, species) values ($1, 1, 'Ração Teste', '{dog}') returning id`, [S1])
check('owner creates product', !!np)
await expectError('owner cannot create product in other store', U.dono,
  `insert into products (store_id, category_id, name) values ('00000000-0000-4000-a000-000000000002', 1, 'x')`, [], 'row-level security')
await expectError('customer cannot create product', U.ana,
  `insert into products (store_id, category_id, name) values ($1, 1, 'x')`, [S1], 'row-level security')
await as(U.dono, `insert into product_variants (product_id, label, price_cents, stock) values ($1,'1 kg',1000,5)`, [np.id])
await as(U.dono, `update products set is_active=false where id=$1`, [np.id])
check('paused product hidden from customers', (await as(U.ana, 'select id from products where id=$1', [np.id])).length === 0)
check('paused product visible to owner', (await as(U.dono, 'select id from products where id=$1', [np.id])).length === 1)

console.log(failures ? `\n${failures} FAILURE(S)` : '\nALL PASSED')
process.exit(failures ? 1 : 0)
