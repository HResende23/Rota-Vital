# 🩸 Rota Vital — Gestão e Distribuição de Hemocomponentes

O **Rota Vital** é uma aplicação web desenvolvida em **Java com Spring Boot** para apoiar o controle, a rastreabilidade e a distribuição de hemocomponentes entre hemocentros e unidades hospitalares.

O projeto busca reduzir desperdícios por vencimento, melhorar a visualização do estoque e apoiar o atendimento de solicitações hospitalares, especialmente em situações de urgência.

---

## 🛠️ Tecnologias utilizadas

### Backend
- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Bean Validation
- Maven

### Banco de dados
- PostgreSQL

### Frontend
- HTML5
- CSS3

### Versionamento e gestão
- Git
- GitHub
- GitHub Issues / Bug Tracker

---

## 📁 Estrutura atual do projeto

```text
Rota-Vital/
├── src/
│   ├── main/
│   │   ├── java/br/com/rotavital/
│   │   │   ├── controller/
│   │   │   │   └── EstoqueController.java
│   │   │   ├── model/
│   │   │   │   ├── BolsaHemoComponente.java
│   │   │   │   ├── StatusBolsa.java
│   │   │   │   ├── TipoComponente.java
│   │   │   │   └── TipoSanguineo.java
│   │   │   ├── repository/
│   │   │   │   └── BolsaHemoComponenteRepository.java
│   │   │   ├── service/
│   │   │   │   └── EstoqueService.java
│   │   │   └── RotaVitalApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── index.html
│   │       │   ├── requisicao.html
│   │       │   ├── alocacao.html
│   │       │   ├── rota.html
│   │       │   └── monitoramento.html
│   │       └── application.properties
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

```
## Estatística e CRISP-DM

A análise estatística do projeto Rota-Vital foi estruturada com base no ciclo CRISP-DM, abordando entendimento do problema, análise dos dados de estoque, preparação, modelagem estatística, avaliação e implantação.

- [Apresentação em PDF](docs/entrega-02/Rota-Vital_Estatistica_CRISP-DM.pdf)
- [Apresentação em PowerPoint](docs/entrega-02/Rota-Vital_Estatistica_CRISP-DM.pptx)
---



### 🎨 Protótipo no Figma
🔗 [Rota Vital — Entrega 01](https://www.figma.com/design/CZ4BYx3COy4SKGpxZ36Zu1/Rota-Vital-%E2%80%94-Entrega-01?node-id=0-1&p=f&t=bXHbJWjJVn23hN8B-0)

## Screencast do Figma
🔗[file:///C:/Users/hilto/Videos/Grava%C3%A7%C3%B5es%20de%20Tela/Grava%C3%A7%C3%A3o%20de%20Tela%202026-10-06%20184734.mp4](https://youtu.be/3hvxZNpkrM4)
---



## 🐛 Issue / Bug Tracker

Durante a Entrega 02 foi utilizado o **GitHub Issues** para registrar histórias, bugs e melhorias do projeto.

Issues trabalhadas:

- `#2` — **[HISTÓRIA] Consultar estoque de hemocomponentes**
- `#3` — **[HISTÓRIA] Cadastrar bolsa de hemocomponente**
- `#4` — **[BUG] Dados do estoque são perdidos ao reiniciar a aplicação**
- `#5` — **[MELHORIA] Padronizar endpoints REST com /api/v1**

As quatro issues foram utilizadas durante o desenvolvimento e encerradas após a validação das respectivas implementações.

### Evidência do Issue/Bug Tracker

![Issues da Entrega 02](docs/entrega-02/issues-entrega-02.png)

---

## 🎥 Screencasts da Entrega 02

> Adicionar os links do YouTube após a gravação.

- **Demonstração do sistema funcionando:** `LINK_AQUI`
- **Explicação do código Spring Boot e das histórias implementadas:** `LINK_AQUI`

---

## ▶️ Como executar o projeto

### Pré-requisitos

Antes de iniciar, instale:

- Java 17 ou superior
- Maven
- PostgreSQL
- Git

### 1. Clonar o repositório

```bash
git clone https://github.com/HResende23/Rota-Vital.git
cd Rota-Vital
```

### 2. Criar o banco de dados

No PostgreSQL, crie um banco chamado:

```text
rotavital
```

### 3. Configurar a senha do banco

O projeto utiliza a variável de ambiente `DB_PASSWORD`.

No PowerShell:

```powershell
$env:DB_PASSWORD="SUA_SENHA_DO_POSTGRES"
```

### 4. Iniciar a aplicação

```bash
mvn spring-boot:run
```

A aplicação será executada em:

```text
http://localhost:8080
```

### 5. Consultar o estoque

```text
http://localhost:8080/api/v1/bolsas
```

---

## 🧪 Exemplo de cadastro de bolsa

Exemplo de JSON:

```json
{
  "codigo": "B001",
  "componente": "CONCENTRADO_HEMACIAS",
  "tipoSanguineo": "A_POSITIVO",
  "validade": "2026-10-20",
  "status": "DISPONIVEL"
}
```

Endpoint:

```http
POST /api/v1/bolsas
```

---

## 👥 Integrantes

| Integrante |
|---|
| Hilton Resende Montes Neto |
| Jardel Simplicio de Oliveira Junior |
| Dayanne Cristina Moraes Inacio |
| Rodrigo Cavalcanti Albuquerque Rodrigues dos Santos |

---

## 📌 Observação

O Rota Vital está em desenvolvimento incremental. As demais histórias previstas no escopo serão implementadas nas próximas entregas.
## Vídeo de apresentação do Rota Vital

Assista à demonstração do projeto pelo link abaixo:

[Assistir ao vídeo](https://drive.google.com/file/d/1nVxMmE_J_bPVI7s0wxBI1vTj-OuuL3cz/view?usp=sharing)
