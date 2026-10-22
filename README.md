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
## Executar com Docker

Pré-requisitos: Git e Docker com o plugin Compose. No Windows, instale e abra
o Docker Desktop antes de executar os comandos. Não é necessário instalar Java,
Maven ou PostgreSQL na máquina para usar este modo.

```bash
git clone https://github.com/HResende23/Rota-Vital.git
cd Rota-Vital
docker compose up --build
```

Se já possui o projeto, execute apenas o último comando na pasta que contém
`Dockerfile`, `compose.yaml` e `pom.xml`. A primeira execução baixa as imagens e
as dependências, podendo levar alguns minutos. Libere a porta 8080 caso outra
instância da aplicação esteja utilizando-a.

O Dockerfile compila o projeto e executa `mvn -B -ntp clean verify` antes de gerar
a imagem da aplicação. Uma falha nos testes interrompe o build. Se as entradas
do build não mudarem, o Docker pode reutilizar essa etapa do cache.

O Compose inicia dois serviços: `db` (PostgreSQL 17) e `app` (Spring Boot com
Java 17). A aplicação aguarda o banco ficar saudável e conecta-se a
`jdbc:postgresql://db:5432/rotavital`. O banco do container é independente de um
PostgreSQL instalado na máquina e não publica sua porta 5432 no host.

Quando aparecer `Started RotaVitalApplication`, acesse
[http://localhost:8080](http://localhost:8080). Mantenha o terminal aberto;
`Ctrl + C` encerra a execução em primeiro plano.

### Comandos úteis

Execute na pasta do projeto. Se o terminal estiver acompanhando os logs, abra
outro terminal nessa mesma pasta.

```bash
# Iniciar em segundo plano, liberando o terminal
docker compose up --build -d

# Consultar os containers e suas situações
docker compose ps

# Ver as últimas mensagens da aplicação
docker compose logs --tail=50 app

# Encerrar e remover os containers e a rede do projeto
docker compose down
```

Os dados do PostgreSQL ficam no volume `postgres_data` e são preservados por
`docker compose down`. O comando `docker compose down -v` também apaga o volume
e seus dados; use-o somente quando quiser limpar o banco deste ambiente.

As credenciais `postgres` / `rotavital_local` presentes no Compose são destinadas
ao desenvolvimento local. A conexão definida no Compose substitui a configuração
local de banco da aplicação.

### Testes automatizados

Com Java compatível instalado, execute a suíte completa sem iniciar os containers:

```powershell
# Windows PowerShell
.\mvnw.cmd clean verify
```

```bash
# Git Bash, Linux ou macOS
sh mvnw clean verify
```

Os testes usam H2 em modo PostgreSQL e não alteram o banco da aplicação.
A suíte desta entrega contém 249 testes. Consulte
[a documentação da refatoração e dos testes](docs/backend-tdd.md) para detalhes.

### Versão da entrega

A tag [ENTREGA-SO](https://github.com/HResende23/Rota-Vital/tree/ENTREGA-SO)
identifica a versão que reúne os testes e a refatoração do backend, a
conteinerização com Docker e Compose e estas instruções de execução.

## Estatística e CRISP-DM

A análise estatística do projeto Rota-Vital foi estruturada com base no ciclo CRISP-DM, abordando entendimento do problema, análise dos dados de estoque, preparação, modelagem estatística, avaliação e implantação.

- [Apresentação em PDF](docs/entrega-02/Rota-Vital_Estatistica_CRISP-DM.pdf)
- [Apresentação em PowerPoint](docs/entrega-02/Rota-Vital_Estatistica_CRISP-DM.pptx)
---



### 🎨 Protótipo no Figma
🔗 [Rota Vital — Entrega 01](https://www.figma.com/design/CZ4BYx3COy4SKGpxZ36Zu1/Rota-Vital-%E2%80%94-Entrega-01?node-id=0-1&p=f&t=bXHbJWjJVn23hN8B-0)


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

## 🎥 Screencasts da Entrega 03

https://drive.google.com/file/d/1nVxMmE_J_bPVI7s0wxBI1vTj-OuuL3cz/view?usp=sharing

## 👥 Integrantes

| Integrante |
|---|
| Hilton Resende Montes Neto |
| Jardel Simplicio de Oliveira Junior |
| Dayanne Cristina Moraes Inacio |
| Rodrigo Cavalcanti Albuquerque Rodrigues dos Santos |


### Usuários e vínculo com requisições

Cadastre um solicitante em `POST /api/v1/usuarios`:

```json
{"nome":"Ana Silva","email":"ana@example.com"}
```

A resposta retorna `201`, o ID e o cabeçalho `Location`. Consulte o cadastro com
`GET /api/v1/usuarios/{id}`. O e-mail é normalizado para minúsculas e deve ser único
(duplicatas retornam `409`); dados inválidos retornam `400`.

Novas requisições exigem um usuário existente:

```json
{
  "usuarioId": 1,
  "hospital": "Hospital Aurora",
  "componente": "CONCENTRADO_HEMACIAS",
  "tipoSanguineo": "A_POSITIVO",
  "quantidade": 2
}
```

Envie para `POST /api/v1/requisicoes`. Omitir `usuarioId`, enviar nulo ou um valor
não positivo retorna `400`; usuário inexistente retorna `404`. A resposta de
cadastro, consulta e reserva inclui `usuarioId`.

A entidade `User` é persistida na tabela `usuarios`; `requisicao.usuario_id`
é uma chave estrangeira com relação muitos-para-um. A coluna aceita nulo somente
para preservar registros antigos; o fluxo de cadastro exige vínculo em toda nova
requisição. Não há exclusão em cascata de usuário nem cadastro implícito durante
a criação de requisição. Com o atual `ddl-auto=update`, o Hibernate cria a tabela
e adiciona a coluna ao iniciar a aplicação; faça backup antes de atualizar uma
base com dados importantes.

Este cadastro identifica o solicitante, mas ainda não implementa autenticação:
o ID informado não comprova identidade. Login, senha e autorização precisam de
uma etapa própria antes de disponibilizar dados reais.
