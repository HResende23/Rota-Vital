# Melhorias do backend a partir do log do Docker

Este documento registra a última implementação da Entrega 04: os ajustes de banco de dados e de
consultas feitos depois da primeira execução do Rota Vital no Docker, com PostgreSQL 17. As
telas de Rota e Monitoramento estão descritas em [historias.md](historias.md).

## O que o log mostrou

O Dockerfile só gera a imagem depois que `mvn clean verify` passa. Por isso, a mensagem
`Started RotaVitalApplication` no log já confirma duas coisas: a suíte de testes passou no Maven
com Java 17, e as tabelas novas (`remessa` e `leitura_temperatura`) foram criadas no PostgreSQL,
com os horários no fuso de Recife (-03:00).

| O que aparece | Leitura | Ação |
|---|---|---|
| `alter table ... add constraint ... foreign key` | O PostgreSQL cria a chave estrangeira, mas não cria índice para ela | Índices nas chaves estrangeiras |
| `WARN ... spring.jpa.open-in-view is enabled by default` | A conexão com o banco ficava aberta enquanto a página era montada | `open-in-view=false` |
| SQL das listagens, impresso pelo `show-sql` ao abrir as telas | Cada item listado disparava mais uma consulta (problema N+1) | `join fetch` e `@EntityGraph` |
| `HHH000247 ... does not exist, skipping` | Aviso inofensivo, explicado abaixo | Nenhuma |
| Colunas de enum como `varchar(255)` | Sem efeito prático no PostgreSQL, explicado abaixo | Nenhuma |

## 1. Índices nas chaves estrangeiras

O PostgreSQL cria índice automaticamente para a chave primária e para colunas `unique`, mas não
para chaves estrangeiras. Sem índice, filtrar por uma chave estrangeira obriga o banco a ler a
tabela inteira, e isso piora à medida que os dados crescem.

| Índice | Tabela (colunas) | Atende |
|---|---|---|
| `idx_leitura_remessa_horario` | `leitura_temperatura (remessa_id, registrada_em)` | Leituras de uma remessa, já em ordem cronológica, e a exclusão em cascata da remessa |
| `idx_requisicao_usuario` | `requisicao (usuario_id)` | Requisições do usuário logado, usadas nas telas de requisição, alocação, rota e monitoramento |
| `idx_requisicao_bolsa_requisicao` | `requisicao_bolsa (requisicao_id)` | Bolsas reservadas de cada requisição |

O índice das leituras é **composto**. As leituras são sempre buscadas por remessa e exibidas em
ordem de horário (`@OrderBy("registradaEm ASC, id ASC")`), então um único índice
`(remessa_id, registrada_em)` resolve o filtro e a ordenação na mesma varredura.

Os índices são declarados nas próprias entidades, e o Hibernate os cria:

```java
@Table(name = "leitura_temperatura",
    indexes = @Index(name = "idx_leitura_remessa_horario", columnList = "remessa_id, registrada_em"))
public class LeituraTemperatura { ... }
```

Com `ddl-auto=update`, um banco que já existe recebe os índices na subida seguinte, sem perder
dados. O DDL gerado para o PostgreSQL é:

```sql
create index idx_leitura_remessa_horario on leitura_temperatura (remessa_id, registrada_em);
create index idx_requisicao_usuario on requisicao (usuario_id);
create index idx_requisicao_bolsa_requisicao on requisicao_bolsa (requisicao_id);
```

## 2. Listagens sem N+1

O problema N+1 acontece quando uma consulta traz N registros e, depois, cada registro dispara mais
uma consulta para carregar uma associação. A listagem de remessas buscava as remessas numa
consulta e, em seguida, a requisição de cada uma, uma por vez:

```sql
select ... from remessa r join requisicao q on ... where q.usuario_id = ?   -- 1 consulta
select ... from requisicao where id = ?                                     -- + 1 por remessa
select ... from requisicao where id = ?
select ... from requisicao where id = ?
```

Com `join fetch`, a requisição vem junto com a remessa, numa consulta só:

```java
@Query("""
    select r from Remessa r join fetch r.requisicao q
    where q.usuario.id = :usuarioId
    order by r.id desc""")
List<Remessa> listarDoUsuario(@Param("usuarioId") Long usuarioId);
```

A listagem de requisições tinha o mesmo problema com as bolsas reservadas e foi corrigida com
`@EntityGraph`:

```java
@EntityGraph(attributePaths = "bolsas")
List<Requisicao> findByUsuarioIdOrderByIdDesc(Long usuarioId);
```

| Listagem | Telas | Antes (3 itens) | Depois |
|---|---|---|---|
| Remessas do usuário | Rota e Monitoramento | 4 consultas (1 + N) | 1 consulta |
| Requisições do usuário | Requisição e Alocação | 4 consultas (1 + N) | 1 consulta |

Com 3 itens a diferença é pequena. Com 100 remessas, seriam 101 consultas a cada abertura da tela.

## 3. `open-in-view` desligado

Por padrão, o Spring Boot mantém a sessão do banco aberta até o fim da requisição HTTP (*Open
Session in View*), inclusive enquanto o Thymeleaf monta a página. Isso esconde consultas: uma tela
que acesse uma associação ainda não carregada consulta o banco no meio da renderização, sem que
ninguém perceba. É por isso que o Spring avisa no log.

Com `spring.jpa.open-in-view=false`, o banco só é consultado dentro dos métodos `@Transactional`
dos serviços. Este projeto já trabalhava assim: os serviços entregam DTOs prontos
(`RemessaResposta`, `RequisicaoResposta` e outros), e nenhuma tela depende de carregamento
tardio. Os testes rodam com a mesma configuração, então uma tela que voltasse a depender disso
falharia nos testes que renderizam as telas.

## O que ficou como está, de propósito

- **`HHH000247 ... does not exist, skipping`:** antes de criar a restrição única de
  `remessa.requisicao_id`, o Hibernate tenta removê-la (`drop constraint if exists`), e o
  PostgreSQL avisa que ela ainda não existia. O aviso só aparece quando a restrição é criada
  pela primeira vez.
- **`varchar(255)` nas colunas de enum:** é o padrão do Hibernate e o mesmo usado no restante do
  projeto. No PostgreSQL, o tamanho declarado de um `varchar` não muda o espaço ocupado, e o
  `CHECK` gerado já limita os valores aos do enum. Além disso, o `update` não altera colunas
  existentes: diminuir o tamanho só deixaria bancos novos diferentes dos que já existem.
- **`show-sql=true`:** continua ligado, porque é ele que mostra o SQL no log, e foi assim que os
  problemas acima apareceram.

## Como foi verificado

1. **Teste antes da correção.** `ConsultasIntegrationTests` conta os comandos SQL de cada
   listagem (com as estatísticas do Hibernate) e confere os índices no banco. Escrito antes da
   correção, ele falhou mostrando 4 consultas e os índices ausentes; depois da correção, passou.
2. **DDL do PostgreSQL.** O schema foi gerado com o dialeto do PostgreSQL, e os três
   `create index` saíram como esperado.
3. **Aplicação em execução.** Todas as telas foram percorridas com dados e com `open-in-view`
   desligado, sem nenhuma exceção no log. O aviso do `open-in-view` deixou de aparecer.
4. **Suíte completa:** 338 testes, 0 falhas.

## Como conferir no seu ambiente

1. Na pasta do projeto, rode `docker compose up --build`.
2. No log, procure as linhas `create index idx_...`, que aparecem na primeira subida com esta
   versão, e confirme que o aviso `spring.jpa.open-in-view is enabled by default` sumiu.
3. Para ver os índices direto no banco, com o Compose rodando:

```bash
docker compose exec db psql -U postgres -d rotavital -c "\di idx_*"
```

## Arquivos alterados

Os caminhos são relativos a `src/main/java/br/com/rotavital`, exceto os de configuração e testes.

| Arquivo | Mudança |
|---|---|
| `model/LeituraTemperatura.java` | Índice `idx_leitura_remessa_horario` |
| `model/Requisicao.java` | Índices `idx_requisicao_usuario` e `idx_requisicao_bolsa_requisicao` |
| `repository/RemessaRepository.java` | `join fetch` na listagem de remessas |
| `repository/RequisicaoRepository.java` | `@EntityGraph` na listagem de requisições |
| `src/main/resources/application.properties` e `src/test/resources/application.properties` | `spring.jpa.open-in-view=false` |
| `src/test/java/br/com/rotavital/ConsultasIntegrationTests.java` (novo) | Contagem de consultas e verificação dos índices |
