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
## 🛠️ Tecnologias Utilizadas

### Monólito (Backend & Frontend)
* **Java 17+** como linguagem de desenvolvimento principal.
* **Spring Boot** para orquestração da aplicação (Spring Web, Spring Data JPA).
* **HTML5 / CSS3** inseridos de forma nativa para a interface do usuário.
* **Maven** como gerenciador de dependências e automação de compilação.

### Banco de Dados e Infraestrutura
* **PostgreSQL** como banco de dados relacional principal.
* **Hibernate / JPA** para mapeamento objeto-relacional (ORM).

------------------------------
## 📁 Estrutura do Projeto

```text
rota-vital-springboot/
└── rota-vital/
    ├── apps/
    │   └── src/
    │       ├── main/
    │       │   ├── java/br/com/rotavital/
    │       │   │   ├── controller/            # Controladores REST (Ex: EstoqueController.java)
    │       │   │   ├── model/                 # Modelagem biológica (Entidades e Enums)
    │       │   │   │   ├── BolsaHemoComponente.java
    │       │   │   │   ├── StatusBolsa.java
    │       │   │   │   ├── TipoComponente.java
    │       │   │   │   └── TipoSanguineo.java
    │       │   │   ├── service/               # Camada de lógica de negócio (EstoqueService.java)
    │       │   │   └── RotaVitalApplication.java # Inicializador da aplicação Spring Boot
    │       │   └── resources/
    │       │       ├── static/                # Interface Frontend do ecossistema
    │       │       │   ├── css/               # Estilizações das páginas
    │       │       │   ├── alocacao.html      # Tela de cruzamento imunológico (T03)
    │       │       │   ├── index.html         # Painel principal/Estoque (T01)
    │       │       │   ├── monitoramento.html # Painel de telemetria térmica IoT (T05)
    │       │       │   ├── requisicao.html    # Tela de pedidos de hospitais (T02)
    │       │       │   └── rota.html          # Painel de roteirização e logística (T04)
    │       │       └── application.properties # Parametrizações do sistema e conexão com Postgres
    │       └── test/                             # Ambiente de testes automatizados
    ├── .mvn/wrapper                               # Executáveis locais do Maven Wrapper
    └── maven-wrapper.properties                   # Declaração de versão do Maven
```

---

```markdown
## 🗺️ Rotas de Acesso e Endpoints Principais

### Painéis da Interface (Frontend)
Qualquer usuário local pode acessar as telas do sistema mapeadas nos arquivos estáticos através do navegador:
* Painel Principal (Estoque): `http://localhost:8080/index.html`
* Painel de Requisições: `http://localhost:8080/requisicao.html`
* Painel de Alocação Imunológica: `http://localhost:8080/alocacao.html`
* Painel de Roteirização Logística: `http://localhost:8080/rota.html`
* Painel de Monitoramento IoT: `http://localhost:8080/monitoramento.html`

### Endpoints da API (`EstoqueController`)
* `GET /api/estoque` - Retorna a listagem de todas as bolsas disponíveis ordenadas por vencimento (Lógica FEFO).
* `POST /api/estoque` - Cadastra uma nova bolsa de hemocomponente inspecionada.
* `GET /api/estoque/alertas` - Filtra e expõe lotes de bolsas biológicas com validade crítica em curto prazo.
```

<FollowUp>
Como os arquivos HTML estão na pasta `static`, a comunicação com o backend provavelmente é feita via chamadas JavaScript (`fetch` ou `axios`). Se você quiser, posso ajudar desenvolvendo:
* O código de **Script JavaScript** para incluir em um dos HTMLs para consumir a listagem do backend.
* A implementação em código Java da entidade **`BolsaHemoComponente.java`** mapeada com JPA para o PostgreSQL.
* O método lógico dentro de **`EstoqueService.java`** estruturando a ordenação de estoque usando a regra **FEFO**.

Qual dessas frentes deseja implementar agora?
</FollowUp>


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


# Especificação Técnica de Histórias de Usuário

Este documento mapeia os requisitos de negócio, fluxos lógicos e critérios de aceite aplicados ao desenvolvimento das funcionalidades do ecossistema **Rota Vital**.

---

### 🩸 T01 — HU01 — Estoque
**Descrição:** Gestão biológica rigorosa de entrada, classificação e ciclo de vida de bolsas de sangue.
* **Escopo Técnico:**
  * Cadastro detalhado de bolsas informando volume (ml), tipo sanguíneo (A, B, AB, O), Fator Rh (+/-), fenótipos raros e data de extração/vencimento.
  * Implementação estrita do modelo **FEFO** (*First Expired, First Out*): o sistema prioriza automaticamente a saída de bolsas com vencimento mais próximo para evitar desperdício de material biológico.
  * Alertas visuais na interface administrativa para lotes em estado crítico de validade.

### 📥 T02 — HU02 — Requisição
**Descrição:** Canal centralizado de entrada de demandas hospitalares com fluxos prioritários.
* **Escopo Técnico:**
  * Interface para hospitais credenciados preencherem requisições de hemocomponentes.
  * Divisão clara em dois fluxos de atendimento:
    * **Rotina:** Segue a ordem de chegada padrão da fila de triagem.
    * **Urgência Máxima:** Ignora a fila comum, dispara notificações sonoras/visuais no painel do hemocentro e bloqueia preventivamente o estoque compatível para atendimento imediato.

### 🧠 T03 — HU03 — Alocação
**Descrição:** Sistema especialista de cruzamento inteligente e compatibilidade imunológica.
* **Escopo Técnico:**
  * Motor de regras automatizado que avalia a requisição do hospital versus o estoque disponível.
  * Validação eletrônica de compatibilidade (Ex: se o pedido solicita sangue tipo `A-`, o sistema restringe a alocação estritamente para bolsas `A-` ou `O-`).
  * Bloqueio físico-lógico da bolsa após alocação bem-sucedida, impedindo que ela seja selecionada por outro operador enquanto aguarda o despacho.

### 🗺️ T04 — HU04 — Rota
**Descrição:** Roteirização multiponto e cálculo do tempo logístico para o transporte de emergência.
* **Escopo Técnico:**
  * Integração com serviços de mapas para gerar rotas dinâmicas a partir do hemocentro central até a unidade hospitalar de destino.
  * Algoritmo de cálculo de tempo crítico baseado em tráfego atual e distância física.
  * Suporte a rotas multiponto, permitindo planejar uma única viagem de entrega otimizada que atenda a múltiplos hospitais vizinhos na mesma saída de frota.

### ❄️ T05 — HU05 — Monitoramento
**Descrição:** Telemetria IoT contínua com foco na segurança e preservação da cadeia de frio.
* **Escopo Técnico:**
  * Módulo de recepção de dados via requisições contínuas, simulando a telemetria de sensores de temperatura acoplados às maletas de transporte térmico.
  * Definição de limites seguros de conservação para cada tipo de hemocomponente (ex: Concentrado de Hemácias entre 2°C e 6°C).
  * Emissão de alertas críticos de infração de temperatura para a equipe de logística caso os parâmetros climáticos ideais sejam rompidos durante o trajeto de entrega.

### 🎒 T06 — HU06 — Fracionamento e Processamento de Sangue Total
**Descrição:** Controle da etapa laboratorial onde o sangue total coletado é centrifugado e dividido em múltiplos hemocomponentes.
* **Escopo Técnico:**
  * **Ciclo de Desmembramento:** Interface e lógica para dar baixa em 1 bolsa de "Sangue Total" e gerar automaticamente a entrada de seus subprodutos derivados (geralmente 1 Concentrado de Hemácias, 1 Plasma Fresco Congelado e 1 Concentrado de Plaquetas).
  * **Rastreabilidade por Código de Barras Matriz:** Vinculação do código identificador único da bolsa mãe de sangue total a todas as sub-bolsas geradas no processo, garantindo que o histórico do doador permaneça atrelado aos derivados.
  * **Regras de Validade Diferenciadas:** Aplicação de regras automáticas de expiração distintas por tipo de hemocomponente gerado (ex: Plaquetas expiram em até 5 dias em agitação contínua, enquanto o Plasma pode durar até 1 ano congelado).

### 🏷️ T07 — HU07 — Triagem e Validação Sorológica
**Descrição:** Bloqueio de segurança e liberação de bolsas para o estoque geral apenas após a inserção e validação de exames laboratoriais negativos.
* **Escopo Técnico:**
  * **Estoque em Quarentena:** Toda bolsa recém-coletada entra nativamente com o status "Em Quarentena", ficando invisível e impossibilitada de ser alocada ou despachada para os hospitais.
  * **Módulo de Laudos Sorológicos:** Tela para inserção dos resultados dos testes obrigatórios (HIV, Hepatite B e C, Sífilis, Chagas e HTLV).
  * **Liberação ou Descarte Automatizado:** 
    * Se **todos** os testes forem negativos, o sistema altera o status da bolsa para "Disponível" (enviando-a para a lógica FEFO).
    * Se **qualquer** teste apresentar resultado reagente/positivo, o sistema bloqueia permanentemente a bolsa, emite um alerta reservado e gera uma ordem de descarte térmico/biológico seguro com registro de protocolo.

## 🎨 Design da Interface (Figma)

O protótipo de alta fidelidade e o mapeamento de experiência do usuário (UX/UI) das telas do sistema (`index.html`, `requisicao.html`, etc.) foram desenvolvidos utilizando o Figma.

* 🔗 **Link do Projeto:** https://www.figma.com/design/CZ4BYx3COy4SKGpxZ36Zu1/Rota-Vital-%25E2%2580%2594-Entrega-01?node-id=0-1&p=f&t=bXHbJWjJVn23hN8B-0


-------

## Integrantes

- Hilton Resende Montes Neto
- Jardel Simplicio de Oliveira Junior
- Dayanne Cristina Moraes Inacio
- Rodrigo Cavalcanti Albuquerque Rodrigues dos Santos
