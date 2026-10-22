-- PetJá: dados fictícios para desenvolvimento.
--
-- Todas as lojas, telefones e endereços são INVENTADOS. As coordenadas ficam em bairros reais
-- de Boa Vista, mas são aproximadas (servem para testar raio e distância, não para navegar).
-- Os produtos usam marcas comuns de petshop só para o catálogo parecer real; preços e estoques
-- são fictícios.
--
-- Cenários cobertos:
--   * Centro, Caçari, Pricumã, Buritis: várias lojas abertas entregam
--   * Santa Tereza: loja fechada (aparece como fechada, não aceita pedido)
--   * Paraviana: loja ainda não aprovada pelo admin (não aparece)
--   * Cidade Satélite: nenhuma parceira entrega -> mostra os petshops não parceiros (RF-15)
--   * Alguns produtos faltam em cada loja e algumas variantes estão com estoque zero (RF-24)
--
-- Rode depois das migrações. O `supabase db reset` faz isso sozinho.

-- ---------------------------------------------------------------------------
-- Categorias (ícones: nomes do Material Symbols)
-- ---------------------------------------------------------------------------
insert into public.categories (name, slug, icon, sort_order) values
    ('Ração',              'racao',      'restaurant',   1),
    ('Petiscos',           'petiscos',   'cookie',       2),
    ('Higiene e limpeza',  'higiene',    'soap',         3),
    ('Brinquedos',         'brinquedos', 'toys',         4),
    ('Acessórios',         'acessorios', 'shopping_bag', 5),
    ('Camas e transporte', 'camas',      'bed',          6),
    ('Aquarismo',          'aquarismo',  'water',        7);

-- ---------------------------------------------------------------------------
-- Lojas parceiras (fictícias)
-- ---------------------------------------------------------------------------
-- IDs fixos para facilitar os testes e o script dev/claim_store.sql.
insert into public.stores (
    id, name, slug, description, phone, whatsapp, street, number, neighborhood, location,
    is_active, is_open, min_order_cents, estimated_minutes,
    delivery_radius_km, base_fee_cents, included_km, extra_km_fee_cents
) values
    ('00000000-0000-4000-a000-000000000001', 'Petshop Lavrado', 'petshop-lavrado',
     'Ração, petiscos e acessórios no coração de Boa Vista.',
     '(95) 3000-0001', '5595900000001', 'Av. Jaime Brasil', '100', 'Centro',
     private.make_point(2.8235, -60.6740), true, true, 3000, 40, 6, 500, 2, 150),

    ('00000000-0000-4000-a000-000000000002', 'Casa do Bicho', 'casa-do-bicho',
     'Tudo para cães e gatos, com entrega rápida no Caçari e região.',
     '(95) 3000-0002', '5595900000002', 'Rua Fictícia do Caçari', '250', 'Caçari',
     private.make_point(2.8335, -60.6775), true, true, 2500, 35, 5, 400, 2, 120),

    ('00000000-0000-4000-a000-000000000003', 'Mundo Pet Pricumã', 'mundo-pet-pricuma',
     'Rações premium e linha completa de higiene.',
     '(95) 3000-0003', '5595900000003', 'Av. Fictícia do Pricumã', '1200', 'Pricumã',
     private.make_point(2.8150, -60.7040), true, true, 4000, 45, 5, 600, 2, 150),

    ('00000000-0000-4000-a000-000000000004', 'AuMiau Buritis', 'aumiau-buritis',
     'Petshop de bairro com preço de depósito.',
     '(95) 3000-0004', '5595900000004', 'Rua Fictícia dos Buritis', '45', 'Buritis',
     private.make_point(2.8240, -60.7160), true, true, 2000, 30, 4, 300, 2, 100),

    ('00000000-0000-4000-a000-000000000005', 'Bicho Feliz', 'bicho-feliz',
     'Ração a granel e acessórios.',
     '(95) 3000-0005', '5595900000005', 'Rua Fictícia de Santa Tereza', '780', 'Santa Tereza',
     private.make_point(2.8040, -60.7210), true, false, 2500, 40, 4, 500, 2, 150),

    ('00000000-0000-4000-a000-000000000006', 'Pet Center Paraviana', 'pet-center-paraviana',
     'Aguardando aprovação.',
     '(95) 3000-0006', '5595900000006', 'Av. Fictícia da Paraviana', '500', 'Paraviana',
     private.make_point(2.8430, -60.6810), false, false, 3000, 40, 5, 500, 2, 150);

-- ---------------------------------------------------------------------------
-- Petshops não parceiros (fictícios), para o RF-15
-- ---------------------------------------------------------------------------
insert into public.external_stores (name, phone, whatsapp, street, neighborhood, location) values
    ('Agropet Satélite',    '(95) 3000-1001', '5595900001001', 'Rua Fictícia A, 10',  'Cidade Satélite',       private.make_point(2.8620, -60.7580)),
    ('Casa da Ração Norte', '(95) 3000-1002', '5595900001002', 'Rua Fictícia B, 220', 'Cauamé',                private.make_point(2.8560, -60.7010)),
    ('Pet Hélio Campos',    '(95) 3000-1003', null,            'Rua Fictícia C, 33',  'Senador Hélio Campos',  private.make_point(2.7950, -60.7500)),
    ('Amigo Bicho',         '(95) 3000-1004', '5595900001004', 'Rua Fictícia D, 90',  'Pintolândia',           private.make_point(2.8000, -60.7350)),
    ('Veterinária e Pet RR','(95) 3000-1005', '5595900001005', 'Rua Fictícia E, 410', 'Jardim Floresta',       private.make_point(2.8400, -60.7050));

-- ---------------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------------
-- Um catálogo base, replicado em cada loja com preço próprio (fator de 0,92 a 1,08),
-- faltando cerca de 1 em cada 5 produtos e com estoque variando por loja.

create temporary table seed_catalog (
    category_slug text,
    name          text,
    brand         text,
    species       public.pet_species[],
    description   text,
    variants      jsonb -- [{"label": "1 kg", "price": 2990}, ...]
);

insert into seed_catalog values
    -- Ração
    ('racao', 'Ração Golden Fórmula Cães Adultos Frango e Arroz', 'Golden', '{dog}',
     'Ração premium especial para cães adultos de todas as raças.',
     '[{"label":"1 kg","price":2990},{"label":"3 kg","price":7490},{"label":"15 kg","price":21990}]'),
    ('racao', 'Ração Premier Raças Pequenas Adultos', 'Premier', '{dog}',
     'Super premium para cães adultos de porte pequeno.',
     '[{"label":"1 kg","price":4290},{"label":"2,5 kg","price":9990},{"label":"7,5 kg","price":25990}]'),
    ('racao', 'Ração Pedigree Adultos Carne e Vegetais', 'Pedigree', '{dog}',
     'Alimento completo para cães adultos.',
     '[{"label":"1 kg","price":2190},{"label":"10,1 kg","price":15990},{"label":"20 kg","price":26990}]'),
    ('racao', 'Ração Special Dog Filhotes', 'Special Dog', '{dog}',
     'Para filhotes de todas as raças.',
     '[{"label":"1 kg","price":1690},{"label":"10,1 kg","price":10990}]'),
    ('racao', 'Ração GranPlus Gatos Castrados Salmão', 'GranPlus', '{cat}',
     'Para gatos castrados, com controle de peso.',
     '[{"label":"1 kg","price":3990},{"label":"3 kg","price":10490},{"label":"10,1 kg","price":28990}]'),
    ('racao', 'Ração Whiskas Gatos Adultos Carne', 'Whiskas', '{cat}',
     'Alimento completo para gatos adultos.',
     '[{"label":"1 kg","price":2490},{"label":"2,7 kg","price":5990},{"label":"10,1 kg","price":18990}]'),
    ('racao', 'Sachê Whiskas Carne ao Molho', 'Whiskas', '{cat}',
     'Alimento úmido para gatos adultos.',
     '[{"label":"85 g","price":349},{"label":"Caixa com 12","price":3990}]'),
    ('racao', 'Mistura de Sementes para Calopsita', 'Alcon', '{bird}',
     'Mistura balanceada para calopsitas.',
     '[{"label":"500 g","price":1890}]'),

    -- Petiscos
    ('petiscos', 'Bifinho Keldog Carne', 'Keldog', '{dog}',
     'Petisco macio sabor carne.',
     '[{"label":"65 g","price":890},{"label":"500 g","price":4990}]'),
    ('petiscos', 'Osso Natural Nó 10 cm', null, '{dog}',
     'Osso para roer, ajuda na limpeza dos dentes.',
     '[{"label":"Unidade","price":990}]'),
    ('petiscos', 'Petisco Dreamies Gatos Queijo', 'Dreamies', '{cat}',
     'Crocante por fora, cremoso por dentro.',
     '[{"label":"40 g","price":990}]'),

    -- Higiene e limpeza
    ('higiene', 'Tapete Higiênico Super Secão', 'Super Secão', '{dog}',
     'Tapete com gel superabsorvente.',
     '[{"label":"7 unidades","price":1990},{"label":"30 unidades","price":5990}]'),
    ('higiene', 'Areia Higiênica Pipicat Classic', 'Pipicat', '{cat}',
     'Areia sanitária para gatos.',
     '[{"label":"4 kg","price":1990},{"label":"12 kg","price":4990}]'),
    ('higiene', 'Shampoo Neutro Sanol Dog', 'Sanol', '{dog,cat}',
     'Para todos os tipos de pelo.',
     '[{"label":"500 ml","price":1990}]'),

    -- Brinquedos
    ('brinquedos', 'Bolinha de Borracha Maciça', null, '{dog}',
     'Resistente, para cães que adoram morder.',
     '[{"label":"Unidade","price":1290}]'),
    ('brinquedos', 'Varinha com Penas', null, '{cat}',
     'Para brincar de caçar.',
     '[{"label":"Unidade","price":1490}]'),
    ('brinquedos', 'Corda Mordedor 3 Nós', null, '{dog}',
     'Brinquedo de corda para puxar e morder.',
     '[{"label":"Unidade","price":1990}]'),

    -- Acessórios
    ('acessorios', 'Coleira Ajustável de Nylon', null, '{dog,cat}',
     'Coleira com fivela de engate rápido.',
     '[{"label":"P","price":1990},{"label":"M","price":2490},{"label":"G","price":2990}]'),
    ('acessorios', 'Comedouro Inox Antiderrapante', null, '{dog,cat}',
     'Base de borracha que não escorrega.',
     '[{"label":"500 ml","price":2490},{"label":"1,5 L","price":3990}]'),

    -- Camas e transporte
    ('camas', 'Cama Retangular Lavável', null, '{dog,cat}',
     'Tecido removível, pode ir na máquina.',
     '[{"label":"P","price":7990},{"label":"M","price":11990},{"label":"G","price":15990}]'),
    ('camas', 'Caixa de Transporte N.2', null, '{dog,cat}',
     'Para pets de até 8 kg.',
     '[{"label":"Unidade","price":11990}]'),

    -- Aquarismo
    ('aquarismo', 'Ração Tetra TetraMin Flocos', 'Tetra', '{fish}',
     'Alimento em flocos para peixes tropicais.',
     '[{"label":"20 g","price":2190}]'),
    ('aquarismo', 'Condicionador de Água', 'Tetra', '{fish}',
     'Remove cloro e metais pesados da água.',
     '[{"label":"100 ml","price":2490}]');

-- Produtos: cada loja recebe o catálogo, menos ~20% dos itens (escolhidos de forma determinística).
insert into public.products (store_id, category_id, name, description, brand, species)
select s.id, c.id, sc.name, sc.description, sc.brand, sc.species
from public.stores s
cross join seed_catalog sc
join public.categories c on c.slug = sc.category_slug
where abs(hashtext(s.slug || sc.name)) % 5 <> 0;

-- Variantes: preço da loja = preço base x fator da loja, terminando em ,90.
-- Estoque de 0 a 24 (o 0 testa o "indisponível").
insert into public.product_variants (product_id, label, price_cents, stock, sort_order)
select
    p.id,
    v.value ->> 'label',
    greatest(
        190,
        (round((v.value ->> 'price')::integer * (0.92 + (abs(hashtext(s.slug)) % 17) / 100.0) / 100.0) * 100 - 10)::integer
    ),
    abs(hashtext(s.slug || p.name || (v.value ->> 'label'))) % 25,
    v.ordinality - 1
from public.products p
join public.stores s on s.id = p.store_id
join seed_catalog sc on sc.name = p.name
cross join lateral jsonb_array_elements(sc.variants) with ordinality as v(value, ordinality);

drop table seed_catalog;
