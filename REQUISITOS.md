# Xodó Pet: levantamento de requisitos

Documento vivo. O objetivo é listar tudo o que o projeto precisa para nada passar batido, separando o que entra no MVP do que fica para depois. Cada requisito tem um código (RF, RN, RNF) para ser citado em issues, commits e conversas.

**Legenda de prioridade**
- **MVP**: sem isso não dá para testar com usuários reais
- **V2**: logo depois de validar
- **Futuro**: só se o projeto ganhar tração

---

## 1. Visão geral

### 1.1 Problema
Em Roraima (Boa Vista no início) não existe um app que reúna petshops locais com entrega rápida. Hoje o tutor compra no balcão ou pelo WhatsApp de cada loja, e os e-commerces nacionais chegam caros e demorados no estado.

### 1.2 Proposta
Um app Android em formato de **marketplace (vitrine de lojas)**, no estilo do Zé Delivery. O cliente informa onde está, vê os petshops que atendem a região, monta o carrinho e recebe em casa. A loja parceira mantém o estoque e faz a entrega.

### 1.3 Objetivos do projeto
1. Começar como **projeto pessoal** para aprender e validar a ideia
2. Ter uma base técnica que aguente virar negócio sem reescrever
3. Validar com um número pequeno de lojas e clientes reais antes de oficializar (CNPJ, contratos)

### 1.4 Fora do escopo agora
- iOS e web para o cliente (só Android; a web é só para o painel do lojista)
- Estoque próprio ou dark store (o estoque é dos parceiros)
- Frota de entregadores da plataforma (no MVP, a loja entrega com o próprio entregador, que usa o modo entregador do app para o rastreio; ver seção 3.9)
- Pagamento online dentro do app (no MVP, pagamento na entrega)
- Venda de medicamentos com receita (ver seção 6)
- Serviços como banho e tosa e leva e traz (V2; ver seção 3.10)

### 1.5 Fase atual
Projeto pessoal com **dados mockados**: lojas, produtos e pedidos fictícios no banco. Parceiros reais só depois que o fluxo estiver completo.

---

## 2. Atores

| Ator | Quem é | Como usa |
|---|---|---|
| **Cliente** | Tutor de pet em Boa Vista | App Android |
| **Lojista** | Dono ou funcionário do petshop parceiro | **Painel web** (para quem tem computador) e **modo lojista** no mesmo app Android (para quem só tem celular) |
| **Entregador** | Motoboy da loja | **Modo entregador** no mesmo app Android: recebe a entrega e compartilha a localização durante o trajeto |
| **Admin** | Você | Painel web (o mesmo do lojista, com permissões de admin); painel do Supabase enquanto o painel não existir |

> **Um app só, com papéis.** Cliente, lojista e entregador usam o mesmo APK; o que aparece depende do papel da conta. Uma pessoa pode ter mais de um papel (o dono da loja também compra para o próprio pet).

---

## 3. Requisitos funcionais

### 3.1 Cliente: conta e acesso
| Código | Requisito | Prioridade |
|---|---|---|
| RF-01 | Navegar pela vitrine **sem login** | MVP |
| RF-02 | Criar conta e entrar com e-mail e senha ou com Google | MVP |
| RF-03 | Entrar com número de telefone (OTP por SMS) | V2 |
| RF-04 | Recuperar a senha | MVP |
| RF-05 | Editar o perfil: nome e telefone | MVP |
| RF-06 | Excluir a conta e os dados (exigência da LGPD e da Play Store) | MVP |

### 3.2 Cliente: endereço e área de entrega
| Código | Requisito | Prioridade |
|---|---|---|
| RF-10 | Cadastrar endereço (rua, número, bairro, complemento, referência) | MVP |
| RF-11 | Confirmar o endereço **num pino no mapa**, gravando latitude e longitude | MVP |
| RF-12 | Salvar vários endereços (casa, trabalho) | V2 |
| RF-13 | Usar a localização do GPS para sugerir o endereço | MVP |
| RF-14 | Mostrar aviso claro quando nenhuma loja parceira entrega no endereço | MVP |
| RF-15 | Quando nenhuma parceira entrega, **mostrar petshops próximos não cadastrados** com telefone/WhatsApp e rota no mapa, deixando claro que não são parceiros | MVP (mockado); V2 (dados reais) |

> Ponto de referência é importante: em muitos bairros de Boa Vista o endereço sozinho não basta para o entregador. O pino no mapa resolve o resto e alimenta o cálculo de distância (RN-02, RN-07) e o rastreio (RF-69).

> **Atendimento:** Boa Vista inteira desde o início. A área de cada loja é definida por **raio de entrega a partir do endereço da loja**, e não por lista de bairros.

> **Lojas não cadastradas (RF-15):** na fase mockada, uma tabela com petshops fictícios. Com dados reais, as opções são a Google Places API (paga por uso, com cota gratuita mensal; os termos proíbem guardar os dados no banco além do `place_id`, então a busca é feita na hora) ou o OpenStreetMap via Overpass (grátis, mas com cobertura mais irregular). Decidir na V2.

### 3.3 Cliente: vitrine e catálogo
| Código | Requisito | Prioridade |
|---|---|---|
| RF-20 | Listar as lojas que entregam no endereço escolhido, das mais próximas para as mais distantes | MVP |
| RF-21 | Mostrar em cada loja: nome, logo, aberta ou fechada, taxa de entrega, tempo estimado e pedido mínimo | MVP |
| RF-22 | Página da loja com produtos agrupados por categoria | MVP |
| RF-23 | Página do produto: fotos, descrição, preço, peso ou tamanho, espécie (cão, gato…) | MVP |
| RF-24 | Esconder ou marcar como indisponível o produto sem estoque | MVP |
| RF-25 | Buscar produtos dentro de uma loja | MVP |
| RF-26 | Buscar produtos em todas as lojas (comparar preço) | V2 |
| RF-27 | Filtrar por espécie, porte, marca e faixa etária (filhote, adulto, sênior) | V2 |
| RF-28 | Banners e promoções na vitrine | V2 |
| RF-29 | Favoritar produtos e lojas | Futuro |
| RF-30 | Variações do mesmo produto (ex.: ração X em 1 kg, 3 kg e 15 kg) | MVP |

### 3.4 Cliente: carrinho
| Código | Requisito | Prioridade |
|---|---|---|
| RF-40 | Adicionar, remover e mudar a quantidade de itens | MVP |
| RF-41 | Carrinho de **uma loja por vez**; ao adicionar de outra loja, perguntar se quer esvaziar | MVP |
| RF-42 | Carrinho persistido no aparelho (não some ao fechar o app) | MVP |
| RF-43 | Mostrar subtotal, taxa de entrega e total | MVP |
| RF-44 | Bloquear o checkout abaixo do pedido mínimo, mostrando quanto falta | MVP |
| RF-45 | Revalidar preço e estoque antes de confirmar o pedido | MVP |

### 3.5 Cliente: checkout e pagamento
| Código | Requisito | Prioridade |
|---|---|---|
| RF-50 | Escolher o endereço de entrega | MVP |
| RF-51 | Escolher a forma de pagamento na entrega: dinheiro (com troco para quanto), Pix na entrega, cartão na maquininha | MVP |
| RF-52 | Campo de observação do pedido | MVP |
| RF-53 | Retirar na loja em vez de receber em casa | V2 |
| RF-54 | Pagamento online por Pix e cartão via gateway (Mercado Pago, Asaas, Pagar.me…) | V2 |
| RF-55 | Agendar a entrega para um horário | Futuro |
| RF-56 | Cupons de desconto | Futuro |

### 3.6 Cliente: pedidos
| Código | Requisito | Prioridade |
|---|---|---|
| RF-60 | Acompanhar o status do pedido **em tempo real** | MVP |
| RF-69 | Ver o **entregador no mapa em tempo real** quando o pedido sai para entrega, com nome do entregador e previsão de chegada. Se o entregador não estiver usando o app, mostrar só o status | MVP |
| RF-61 | Histórico de pedidos | MVP |
| RF-62 | Cancelar o pedido enquanto a loja ainda não aceitou | MVP |
| RF-63 | Botão para falar com a loja pelo WhatsApp | MVP |
| RF-64 | **Pedir de novo** com um toque (repete o carrinho) | MVP |
| RF-65 | Notificação push a cada mudança de status | V2 |
| RF-66 | Avaliar a loja e o pedido | V2 |
| RF-67 | **Lembrete de recompra** da ração ("deve acabar em 5 dias") | V2 |
| RF-68 | Assinatura com entrega recorrente | Futuro |

> O lembrete de recompra (RF-67) é o maior diferencial do produto. A ração acaba num ritmo previsível; usar isso é o que fideliza.

### 3.7 Cliente: perfil do pet
| Código | Requisito | Prioridade |
|---|---|---|
| RF-70 | Cadastrar pets: nome, espécie, porte, idade, peso | V2 |
| RF-71 | Sugerir produtos de acordo com o pet | Futuro |
| RF-72 | Calcular a duração do saco de ração pelo peso do pet (alimenta o RF-67) | Futuro |

### 3.8 Lojista
Disponível no **painel web** e no **modo lojista do app**. Onde a coluna "Onde" diz "ambos", as duas interfaces precisam ter a função.

| Código | Requisito | Onde | Prioridade |
|---|---|---|---|
| RF-80 | Entrar com a conta da loja | ambos | MVP |
| RF-81 | Receber o pedido novo com **alerta sonoro** | ambos | MVP |
| RF-82 | Aceitar ou recusar o pedido, informando o motivo | ambos | MVP |
| RF-83 | Atualizar o status: em separação, saiu para entrega, entregue | ambos | MVP |
| RF-84 | Abrir e fechar a loja manualmente | ambos | MVP |
| RF-85 | Cadastrar e editar produtos, preços, fotos (tirar foto pela câmera no app) e estoque | ambos | MVP |
| RF-86 | Pausar um produto em falta | ambos | MVP |
| RF-87 | Configurar raio de entrega, taxa de entrega, pedido mínimo e tempo estimado | ambos | MVP |
| RF-88 | Configurar horário de funcionamento por dia da semana | ambos | V2 |
| RF-89 | Relatório de vendas e comissão do período | web | V2 |
| RF-90 | Importar o catálogo por planilha | web | V2 |
| RF-91 | Cadastrar os entregadores da loja e atribuir um entregador ao pedido | ambos | MVP |
| RF-92 | Ver no mapa onde estão os entregadores com entrega em andamento | ambos | V2 |

> O modo lojista pode reaproveitar o KDS Brasa do Jorge: é o mesmo problema de "pedido chega, muda de etapa, avisa com som".

> **Catálogo inicial:** cadastrado pelo admin ou pelo lojista. Na fase mockada, um script de seed popula lojas e produtos fictícios.

### 3.9 Entregador
Modo entregador no mesmo app. O entregador é da loja no MVP; a plataforma não tem frota própria.

| Código | Requisito | Prioridade |
|---|---|---|
| RF-110 | Entrar com uma conta vinculada a uma loja (convite feito pelo lojista) | MVP |
| RF-111 | Ver as entregas atribuídas a ele, com endereço, referência, valor a cobrar e troco | MVP |
| RF-112 | Abrir a rota no Google Maps ou Waze | MVP |
| RF-113 | Botão "Iniciar entrega": muda o status para SAIU_PARA_ENTREGA e começa a **compartilhar a localização** | MVP |
| RF-114 | Botão "Entregue" (ou "Não entregue", com motivo): para o compartilhamento | MVP |
| RF-115 | Localização compartilhada **só durante a entrega**, com notificação visível enquanto ativa (exigência do Android e da LGPD) | MVP |
| RF-116 | Ligar ou mandar WhatsApp para o cliente | MVP |
| RF-117 | Código de confirmação de entrega (o cliente informa 4 dígitos ao receber) | V2 |
| RF-118 | Várias entregas na mesma saída (rota com mais de um pedido) | Futuro |
| RF-119 | Entregadores parceiros da plataforma, que atendem várias lojas (modelo iFood) | Futuro |

### 3.10 Serviços (V2)
Banho e tosa, clínicas e leva e traz. Anotações para quando chegar a hora:
- Petshops e **clínicas veterinárias** se cadastram como prestadores de serviço, com agenda de horários
- O leva e traz exige **veículo adaptado** para transportar animais (caixa de transporte, ventilação, segurança), o que muda o perfil do entregador
- Precisa de termo de responsabilidade pelo animal durante o transporte
- O rastreio em tempo real (RF-69) vale ainda mais aqui: o tutor quer ver onde o pet está

### 3.11 Admin
| Código | Requisito | Prioridade |
|---|---|---|
| RF-100 | Cadastrar, aprovar e desativar lojas | MVP |
| RF-101 | Cadastrar e editar o catálogo de qualquer loja | MVP |
| RF-102 | Ver todos os pedidos e intervir (cancelar, contatar) | MVP |
| RF-103 | Categorias globais de produto (ração, petiscos, higiene, acessórios, farmácia…) | MVP |
| RF-104 | Relatório de comissão por loja | V2 |
| RF-105 | Painel com as métricas da seção 8 | V2 |

---

## 4. Regras de negócio

| Código | Regra |
|---|---|
| RN-01 | Um pedido pertence a **uma única loja** |
| RN-02 | A loja só aparece para o cliente se estiver ativa, aberta e o endereço estiver dentro do raio de entrega dela |
| RN-03 | O preço do pedido é **congelado** no momento da confirmação; mudanças posteriores no catálogo não alteram pedidos feitos |
| RN-04 | O status do pedido segue uma máquina de estados (abaixo); transição fora dela é recusada |
| RN-05 | O cliente só cancela enquanto o pedido está PENDENTE; depois disso, só a loja ou o admin |
| RN-06 | Pedido PENDENTE por mais de **10 minutos** sem resposta da loja é cancelado automaticamente e o cliente é avisado (o cliente está esperando a entrega, então o prazo é curto) |
| RN-06b | Com pagamento online (V2): pedido com Pix não pago em **30 minutos** é cancelado automaticamente (mesmo prazo usado pela Petz). Os dois prazos são configuráveis |
| RN-07 | A taxa de entrega é definida por loja: valor fixo até X km mais valor por km adicional, até o raio máximo |
| RN-08 | A comissão da plataforma é um percentual sobre o subtotal, sem a taxa de entrega. **Zero durante a fase de teste**; percentual a definir antes de oficializar |
| RN-11 | A localização do entregador só é gravada e exibida enquanto o pedido está SAIU_PARA_ENTREGA; ao finalizar, o cliente perde o acesso e o trajeto não fica guardado |
| RN-09 | Pedido abaixo do mínimo da loja não pode ser finalizado |
| RN-10 | Produto com estoque zero ou pausado não pode ser adicionado ao carrinho |

### 4.1 Máquina de estados do pedido

| De | Para |
|---|---|
| PENDENTE | ACEITO, RECUSADO, CANCELADO |
| ACEITO | EM_SEPARACAO, CANCELADO |
| EM_SEPARACAO | SAIU_PARA_ENTREGA (ou PRONTO_PARA_RETIRADA, na V2), CANCELADO |
| SAIU_PARA_ENTREGA | ENTREGUE, NAO_ENTREGUE |
| ENTREGUE, RECUSADO, CANCELADO, NAO_ENTREGUE | nenhuma (finais) |

Mesmo padrão do `StageMachine` do KDS: tabela no domínio, `when` exaustivo e validação também no banco.

---

## 5. Requisitos não funcionais

| Código | Requisito |
|---|---|
| RNF-01 | Android 8.0 (API 26) ou superior |
| RNF-02 | Funcionar bem em **aparelhos de entrada** e com internet móvel instável: imagens comprimidas, cache, estados de carregamento e de erro em toda tela |
| RNF-03 | Status do pedido atualizado em até poucos segundos (Supabase Realtime) |
| RNF-04 | Segurança no banco com **Row Level Security**: cliente só vê os próprios pedidos; lojista só vê e edita a própria loja |
| RNF-05 | Nenhuma chave secreta no APK; só a chave pública (anon) do Supabase |
| RNF-06 | HTTPS em tudo |
| RNF-07 | Acessibilidade: textos com contraste AA, alvos de toque de 48 dp, suporte a fonte grande e TalkBack nas telas principais |
| RNF-08 | Tema claro e escuro |
| RNF-09 | Textos em português, em arquivos de recurso (pronto para traduzir; há público venezuelano em Boa Vista) |
| RNF-10 | Monitoramento de falhas (Firebase Crashlytics ou Sentry) desde o primeiro teste com usuários |
| RNF-11 | Testes de unidade no domínio (máquina de estados, carrinho, regras de preço) |
| RNF-12 | CI rodando build, ktlint e testes a cada push (como no KDS) |
| RNF-13 | Custo de infraestrutura zero ou próximo de zero na fase pessoal (planos grátis) |

---

## 6. Legal e regulatório

Não precisa estar tudo resolvido para o projeto pessoal, mas precisa estar mapeado antes de abrir para o público.

| Tema | O que fazer | Quando |
|---|---|---|
| **LGPD** | Política de privacidade, consentimento, exclusão de conta, mínimo de dados pessoais | Antes do primeiro usuário real |
| **Termos de uso** | Deixar claro que a plataforma intermedeia e que a loja é responsável pelo produto e pela entrega | Antes do primeiro usuário real |
| **Play Store** | Conta de desenvolvedor (taxa única), política de privacidade publicada, formulário de segurança de dados, exclusão de conta | Antes de publicar |
| **Código de Defesa do Consumidor** | Preço total claro antes de confirmar, canal de atendimento, política de cancelamento e troca | Antes de abrir ao público |
| **Medicamentos veterinários** | Alguns exigem receita e têm regras próprias. **No MVP, não vender medicamentos**; consultar a regra antes de incluir | Antes de habilitar a categoria |
| **Formalização** | CNPJ (MEI tem limite de faturamento e de atividades; validar com contador), contrato com as lojas, nota fiscal (a loja emite a do produto; a plataforma, a da comissão) | Quando decidir oficializar |
| **Pagamento online** | Usar um gateway que faça o split entre loja e plataforma, para você não receber e repassar dinheiro de terceiros | V2 |

---

## 7. Modelo de dados inicial

Entidades do primeiro corte (Postgres no Supabase):

- **profiles**: id (do auth), nome, telefone, admin (sim/não)
- **addresses**: perfil, rua, número, complemento, referência, bairro, latitude, longitude
- **stores**: nome, logo, telefone/WhatsApp, endereço, latitude, longitude, aberta, ativa, pedido mínimo, tempo estimado, raio de entrega (km), taxa base, km incluídos na taxa base, valor por km adicional
- **store_members**: loja ↔ perfil, com o papel na loja (dono, atendente, entregador)
- **external_stores**: petshops não parceiros para o RF-15 (só na fase mockada; com dados reais, a busca é feita na hora)
- **categories**: nome, ícone, ordem
- **products**: loja, categoria, nome, descrição, marca, espécie, fotos
- **product_variants**: produto, rótulo (ex.: "15 kg"), preço, estoque, ativo
- **orders**: cliente, loja, entregador, endereço (cópia, com latitude e longitude), status, subtotal, taxa, total, forma de pagamento, troco, observação, datas
- **order_items**: pedido, variante, nome e preço **copiados**, quantidade
- **order_status_history**: pedido, de, para, quem, quando
- **courier_locations**: pedido, latitude, longitude, direção, atualizado em. **Uma linha por entrega em andamento**, sobrescrita a cada atualização e apagada ao finalizar (RN-11)

V2: pets, reviews, push_tokens, coupons, services, appointments.

> **Distância no banco:** a extensão **PostGIS** (disponível no Supabase) faz a consulta "lojas cujo raio cobre este ponto" direto no Postgres, com índice espacial.

---

## 8. Métricas de validação

O que medir para decidir se vale oficializar:

| Métrica | Por que importa |
|---|---|
| Pedidos por semana | Existe demanda? |
| **Taxa de recompra em 30 e 60 dias** | A principal: ração é recorrente, e se ninguém volta, algo está errado |
| Ticket médio | Paga a entrega e a comissão? |
| Pedidos recusados ou cancelados pela loja | A operação das lojas aguenta? |
| Tempo médio de entrega | A promessa de rapidez se cumpre? |
| Lojas ativas e itens no catálogo | Oferta suficiente para o cliente voltar |

Ferramenta sugerida: Firebase Analytics ou PostHog (plano grátis) mais consultas no próprio banco.

---

## 9. Decisões técnicas

| Tema | Decisão | Motivo |
|---|---|---|
| Plataforma | Android nativo, Kotlin | Experiência do dev; Android domina o público |
| Estrutura | Mesma do KDS: `build-logic`, `core/*`, `feature/*`, domínio em JVM puro | Já validada; facilita um app do lojista no mesmo repositório e uma migração futura para KMP |
| UI | Jetpack Compose + Material 3 | |
| DI | Hilt | Já usado no KDS |
| Backend | Supabase (Auth, Postgres, Storage, Realtime) com `supabase-kt` | Sem servidor próprio; plano grátis |
| Imagens | Coil | |
| Carrinho local | DataStore ou Room | RF-42 |
| Push | Firebase Cloud Messaging | V2 |
| Papéis | Um APK com modos cliente, lojista e entregador | Decisão da seção 11 |
| Mapa no app | Maps SDK for Android (Google) | Mapa no app nativo é gratuito; o cliente já conhece a interface |
| Geocodificação (endereço ↔ coordenada) | Geocoder do Android no MVP; Google Geocoding API se a precisão não bastar | O Geocoder é grátis, mas varia por aparelho |
| Rastreio do entregador | Foreground service de localização (a cada 5 a 10 s, ou a cada 20 m) enviando para o Supabase Realtime | Tempo real sem servidor próprio; ver nota abaixo |
| Distância e raio | PostGIS no Supabase | Consulta espacial com índice |
| Painel web do lojista e admin | **Next.js (React + TypeScript)** com `supabase-js`, hospedado na Vercel (plano grátis) | Stack mais madura para painel e CRUD; usa o mesmo Supabase, as mesmas tabelas e as mesmas regras de RLS do app |

Módulos iniciais: `app`, `core/domain`, `core/data`, `core/designsystem`, `core/ui`, `feature/storefront`. Os demais entram conforme o app cresce: `store`, `cart`, `checkout`, `orders`, `auth`, `merchant` (modo lojista) e `courier` (modo entregador).

> **Rastreio em tempo real:** o mais barato é o entregador mandar a posição por um canal de *broadcast* do Supabase Realtime, que entrega direto para o app do cliente sem gravar cada ponto no banco, e gravar só a última posição em `courier_locations` a cada 30 s ou mais (para quem abre o app no meio da entrega). Isso reduz escrita no banco e fica dentro do plano grátis com folga. No app do cliente, o marcador é animado entre um ponto e outro para não "pular".

---

## 10. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Lojas não querem entrar ("vai roubar meu cliente") | Sem oferta, não há produto | Começar com zero de comissão, mostrar que o pedido é delas e que o app traz cliente novo |
| Loja não atualiza o estoque | Cliente pede o que não existe | Pausa rápida de produto, recusa com motivo, catálogo pequeno no início |
| Loja demora a aceitar | Experiência ruim | Alerta sonoro, cancelamento automático (RN-06) |
| Margem da ração é baixa | Comissão alta afasta a loja | Comissão menor na ração, maior em acessórios e petiscos |
| iFood ou concorrente local já atende | Diferencial fraco | Foco em pet, recompra e lembrete, atendimento local |
| Projeto pessoal perde ritmo | Nunca chega a usuário real | MVP pequeno, metas por fase (seção 12) |
| Entregador da loja não usa o modo entregador | Sem rastreio, que é parte da proposta | Modo entregador simples (dois botões); sem app, o pedido continua funcionando só com status; o lojista pode marcar "saiu" e "entregue" por ele |
| Rastreio gasta bateria e dados do entregador | Entregador desliga o app | Só durante a entrega (RN-11), intervalo de 5 a 10 s, precisão balanceada |

---

## 11. Decisões e perguntas em aberto

### 11.1 Decidido
- [x] **Lojas parceiras:** dados mockados na fase pessoal
- [x] **Área de atendimento:** Boa Vista inteira; cada loja tem um raio de entrega. Sem loja parceira no alcance, mostrar petshops próximos não cadastrados com o contato deles (RF-15)
- [x] **Comissão:** zero durante o teste; percentual a definir antes de oficializar (RN-08)
- [x] **Cancelamento automático:** 10 min para a loja aceitar (RN-06); 30 min para pagar o Pix, quando houver pagamento online (RN-06b)
- [x] **Operação do lojista:** painel web e modo lojista no app (seção 3.8)
- [x] **Apps:** um APK só, com modos cliente, lojista e entregador
- [x] **Serviços:** V2, com petshops e clínicas cadastrados e veículo adaptado para o leva e traz (seção 3.10)
- [x] **Catálogo:** cadastrado pelo admin ou pelo lojista; seed com dados fictícios na fase mockada
- [x] **Nome:** Xodó Pet (seção 13)
- [x] **Painel web:** Next.js com `supabase-js` (seção 9)
- [x] **Entrega:** entregador da própria loja, com rastreio em tempo real pelo modo entregador; entregadores parceiros da plataforma ficam para o futuro (RF-119)

### 11.2 Em aberto
- [ ] Identidade visual do Xodó Pet: logo, paleta e mascote (seção 13)
- [ ] Checar a disponibilidade do nome Xodó Pet (seção 13)
- [ ] Fonte real dos petshops não cadastrados: Google Places ou OpenStreetMap (RF-15, V2)
- [ ] Percentual de comissão, antes de oficializar

---

## 12. Fases

| Fase | Entrega | Pronto quando |
|---|---|---|
| **0. Base** | Projeto com a estrutura do KDS, Supabase com PostGIS, tabelas, RLS e seed de dados fictícios de Boa Vista | App abre e lista lojas fictícias do banco |
| **1. Vitrine** | RF-01, RF-10 a RF-15, RF-20 a RF-25, RF-30 | Cliente marca o endereço no mapa e vê as lojas que entregam ali, ou os petshops não parceiros |
| **2. Pedido** | Conta (RF-02 a RF-06), carrinho (RF-40 a RF-45), checkout (RF-50 a RF-52) | Um pedido é criado no banco a partir do app |
| **3. Operação** | Modo lojista no app (RF-80 a RF-87, RF-91), status em tempo real (RF-60 a RF-64), RN-06 | Pedido feito no app chega à loja e o cliente acompanha o status |
| **4. Rastreio** | Modo entregador (RF-110 a RF-116), mapa ao vivo para o cliente (RF-69), RN-11 | O cliente vê o entregador se movendo no mapa até a porta |
| **5. Painel web** | Funções "ambos" da seção 3.8 na web, mais o admin | Lojista consegue operar só pelo computador |
| **6. Teste real** | Política de privacidade, termos, Crashlytics, publicação em teste fechado na Play Store | 1 a 3 lojas e um grupo de clientes conhecidos usando de verdade |
| **7. V2** | Push, lembrete de recompra, pagamento online, avaliações, serviços | Decidido pelas métricas da fase 6 |

---

## 13. Nome e identidade

**Nome escolhido: Xodó Pet.** "Xodó" é como o tutor chama o próprio bicho: afetivo, bem brasileiro e fácil de lembrar.

### 13.1 Antes de usar publicamente
"Xodó" é uma palavra comum, e é provável que já existam petshops com nomes parecidos em outros estados. Para um projeto pessoal isso não impede nada, mas antes de publicar na Play Store ou oficializar:
- [ ] Pesquisar a marca no **INPI** (classes de comércio de produtos pet e de serviço de entrega)
- [ ] Verificar o domínio (`xodopet.com.br` e variações) e o @ no Instagram
- [ ] Procurar apps com nome parecido na Play Store
- [ ] Se estiver ocupado, variações que mantêm a ideia: **Xodó RR**, **Meu Xodó**, **Xodó Delivery**

### 13.2 Identidade visual (a definir)
- Tom de voz carinhoso, como quem fala com o próprio pet: "seu xodó vai amar", "o pedido do seu xodó saiu para entrega"
- Mascote de vira-lata ou gato, com um toque de Roraima (um buriti, o Monte Roraima ao fundo, o leque de Boa Vista)
- Paleta quente e acolhedora; a do lavrado (dourado do capim, verde do buriti, azul do céu) continua uma boa base
