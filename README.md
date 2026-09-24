## Rota Vital — Gestão e Distribuição de Hemocomponentes
O Rota Vital é uma plataforma robusta de logística corporativa e gerenciamento de saúde voltada para otimizar o fluxo de armazenamento, controle de estoque e distribuição de hemocomponentes (como sangue total, plasma, plaquetas e crioprecipitado) entre hemocentros centrais e unidades hospitalares. O sistema foca em mitigar o desperdício de bolsas por vencimento e acelerar o tempo de resposta em cenários de extrema urgência médica.

## 🚀 Funcionalidades

* Rastreamento em tempo real do volume de hemocomponentes por tipo sanguíneo, fator Rh e fenótipos raros.
* Alertas visuais e notificações automáticas para bolsas com data de validade próxima do vencimento (técnica FIFO/FEFO).

## 📍 Roteirização e Logística de Emergência

* Algoritmo de roteirização integrado que calcula trajetos prioritários entre o hemocentro e os hospitais solicitantes.
* Monitoramento de temperatura simulado ou integrado para garantir a cadeia de frio durante o transporte.

## 🏥 Gestão de Demandas Hospitalares

* Portal do Hospital para a criação de pedidos de rotina ou pedidos de emergência máxima (com prioridade na fila de despacho).
* Fluxo de aprovação e triagem automatizada com base na disponibilidade atual do hemocentro.

## 🔍 Rastreabilidade de Ponta a Ponta

* Histórico completo do ciclo de vida da bolsa: Coleta ➔ Processamento/Fracionamento ➔ Armazenamento ➔ Despacho ➔ Transfusão/Descarte.

------------------------------
## 🛠️ Tecnologias Utilizadas## Backend

* Node.js com TypeScript e framework NestJS.
* TypeORM para mapeamento objeto-relacional.

## Frontend

* React.js com Next.js (App Router).
* Tailwind CSS e Shadcn/ui para componentes de interface.

## Infraestrutura e Bancos de Dados

* PostgreSQL como banco de dados relacional principal.
* Redis para gerenciamento de cache de rotas e controle de sessões.
* Docker e Docker Compose para containerização de ambientes.

------------------------------
## 📁 Estrutura do Projeto

rota-vital/
├── apps/
│   ├── api/                 # Código-fonte do Backend (NestJS)
│   │   ├── src/
│   │   │   ├── modules/     # Estoque, Pedidos, Rotas, Usuários
│   │   │   └── database/    # Migrations e Seeds
│   │   └── Dockerfile
│   └── web/                 # Código-fonte do Frontend (Next.js)
│       ├── src/
│       │   ├── app/         # Páginas e Rotas
│       │   ├── components/  # Componentes de UI refratários
│       │   └── services/    # Integração com a API
│       └── Dockerfile
├── docker-compose.yml       # Orquestração local (Postgres, Redis, APIs)
└── README.md

------------------------------
## 🛑 Pré-requisitos
Antes de iniciar, instale as seguintes ferramentas em seu ambiente de desenvolvimento:

* Git
* [Node.js v18+](https://nodejs.org/)
* [Docker & Docker Compose](https://www.docker.com/)

------------------------------
## ⚙️ Instalação e Execução Local## 1. Clonar o Repositório

git clone https://github.com
cd rota-vital

## 2. Configurar Variáveis de Ambiente
Crie um arquivo .env dentro da pasta apps/api/ e outro dentro de apps/web/ seguindo o modelo dos arquivos .env.example presentes em cada uma delas.
Exemplo essencial para a API (apps/api/.env):

DATABASE_URL=postgresql://postgres:postgres@localhost:5432/rotavital
REDIS_URL=redis://localhost:6379
JWT_SECRET=sua_chave_secreta_aqui

## 3. Execução via Docker (Recomendado)
Para subir todo o ecossistema (Postgres, Redis, API e Web) de forma automatizada:

docker-compose up -d --build

A aplicação estará disponível em:

* Frontend: http://localhost:3000
* Backend API: http://localhost:3333

## 4. Execução Manual (Modo Desenvolvimento)
Caso prefira rodar os serviços fora de containers:
Instalar dependências (na raiz do projeto):

npm install

Executar Migrations do Banco de Dados:

npm run migration:run --workspace=apps/api

Iniciar o Backend:

npm run dev --workspace=apps/api

Iniciar o Frontend:

npm run dev --workspace=apps/web

------------------------------
## 🗺️ Documentação da API (Endpoints Principais)
Todos os endpoints privados exigem o cabeçalho Authorization: Bearer <JWT_TOKEN>.
## Autenticação

* POST /api/auth/login - Realiza a autenticação de usuários (Hemocentro/Hospital).

## Estoque (Hemocentro)

* GET /api/inventory - Lista todas as bolsas de sangue disponíveis.
* POST /api/inventory - Registra a entrada de um novo hemocomponente coletado.
* GET /api/inventory/alerts - Retorna bolsas próximas do vencimento.

## Pedidos (Hospitais)

* POST /api/orders - Cria uma nova solicitação de hemocomponente (Rotina ou Emergência).
* GET /api/orders - Lista os pedidos associados à instituição.
* PATCH /api/orders/:id/status - Atualiza o status do pedido (Pendente, Aprovado, Em Trânsito, Entregue).

------------------------------
## 🚀 Deploy em Produção
Para gerar as builds otimizadas de produção sem Docker:

# Gerar arquivos de produção
npm run build
# Iniciar em modo produção
npm run start

Para deploy em nuvem (AWS, DigitalOcean, GCP), recomenda-se a utilização do arquivo docker-compose.yml configurado com variáveis de ambiente de produção e volumes persistentes para o PostgreSQL.
------------------------------
## 🤝 Como Contribuir

   1. Faça um Fork do projeto.
   2. Crie uma Branch para sua modificação:
   
   git checkout -b feature/minha-modificacao
   
   3. Faça o Commit seguindo o padrão Conventional Commits:
   
   git commit -m "feat(order): adiciona triagem automatizada por urgência"
   
   4. Envie para o seu repositório remoto:
   
   git push origin feature/minha-modificacao
   
   5. Abra um Pull Request detalhando as alterações realizadas.


## 🌟 Histórias:

   T01 — HU01 — Estoque (Controle biológico, tipagem e regras FEFO).
   
   T02 — HU02 — Requisição (Entrada de pedidos e fluxos especiais de urgência).
   
   T03 — HU03 — Alocação (Cruzamento inteligente e compatibilidade imunológica automática).
   
   T04 — HU04 — Rota (Roteirização multiponto e cálculo de tempo logístico crítico).
   
   T05 — HU05 — Monitoramento (Telemetria com sensores IoT para checagem da cadeia de frio).

-------

## Integrantes

- Hilton Resende Montes Neto
- Jardel Simplicio de Oliveira Junior
- Dayanne Cristina Moraes Inacio
- Rodrigo Cavalcanti Albuquerque Rodrigues dos Santos
