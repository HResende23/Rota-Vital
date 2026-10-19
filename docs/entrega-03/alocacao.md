# Entrega 03 — Backend da tela 03: Alocação

Base: `HResende23/Rota-Vital`, commit `8212802946962cbbf01d21bb9062264304c303f9`.
As histórias abaixo são extraídas de `src/main/resources/static/alocacao.html`.
O backend existente da Entrega 02 cadastra e lista bolsas; estas duas histórias acrescentam a proposta e sua confirmação.

## História 03 — Consultar proposta de alocação

Como responsável pela distribuição, quero consultar uma proposta de bolsas compatíveis ordenadas por vencimento, para atender uma requisição e reduzir desperdício.

Critérios de aceitação:
- Dada uma requisição pendente, quando consultar a proposta, então receber somente bolsas do componente solicitado, compatíveis, disponíveis e não vencidas.
- Dadas bolsas elegíveis, quando selecionar a quantidade solicitada, então priorizar a menor validade (FEFO); empates são resolvidos pelo código.
- Dado estoque insuficiente, quando consultar, então informar solicitadas, selecionadas e faltantes, sem modificar o estoque.
- Dada requisição inexistente, retornar 404; se já alocada, retornar 409.

`GET /api/v1/requisicoes/{id}/proposta-alocacao`

## História 04 — Confirmar reserva de bolsas

Como responsável pela distribuição, quero confirmar as bolsas propostas para uma requisição, para garantir sua reserva e registrar quais bolsas atenderão o hospital.

Critérios de aceitação:
- Dada uma seleção completa, distinta e elegível, quando confirmar, então reservar as bolsas, vinculá-las à requisição e mudar a requisição para ALOCADA.
- Dada uma bolsa que ficou indisponível, incompatível ou vencida após a proposta, quando confirmar, então retornar 409 sem reservar parcialmente o lote.
- Dadas duas confirmações concorrentes para a mesma bolsa, somente uma pode ser concluída.
- Dada quantidade incorreta, lista vazia ou códigos duplicados, retornar 400; bolsa inexistente retorna 404.
- Dada requisição já alocada, uma nova confirmação retorna 409 sem alterar a reserva existente.
- A reserva e o vínculo são persistidos na mesma transação.

`POST /api/v1/requisicoes/{id}/reserva`

## Suporte mínimo de requisição

O repositório ainda não possuía requisições persistidas. Foram acrescentados cadastro e consulta simples como pré-requisito técnico das duas histórias. Não implementam todo o fluxo da tela 02 (prioridade, prazo, acompanhamento etc.).

| Método | Caminho | Uso |
|---|---|---|
| POST | `/api/v1/requisicoes` | Criar uma requisição pendente |
| GET | `/api/v1/requisicoes/{id}` | Consultar status e códigos reservados |
| GET | `/api/v1/requisicoes/{id}/proposta-alocacao` | História 03 |
| POST | `/api/v1/requisicoes/{id}/reserva` | História 04 |

## Arquitetura para estudar e explicar

1. `AlocacaoController`: recebe as chamadas HTTP e valida os DTOs de entrada com `@Valid`.
2. `AlocacaoService`: coordena busca, compatibilidade, seleção FEFO e confirmação.
3. `CompatibilidadeService`: centraliza a regra acadêmica de compatibilidade.
4. `RequisicaoRepository` e `BolsaHemoComponenteRepository`: consultam e persistem via JPA.
5. `Requisicao`: representa o pedido, seu status e as bolsas vinculadas.
6. `BolsaHemoComponente.reservar()`: muda o status da bolsa elegível para RESERVADA.

Na confirmação, `@Transactional` mantém a operação inteira na mesma transação. As consultas com `PESSIMISTIC_WRITE` bloqueiam a requisição e as bolsas durante a confirmação. O banco também impede que o mesmo código de bolsa apareça em duas reservas, pela restrição única na tabela `requisicao_bolsa`. Objetos gerenciados pelo JPA são atualizados ao finalizar a transação, sem precisar chamar `save()` para cada mudança.

## Limites explícitos desta implementação

- Compatibilidade didática: concentrado de hemácias usa regras ABO/Rh simplificadas; plasma e plaquetas aceitam somente tipo sanguíneo idêntico. Não é uma matriz clínica completa.
- A validade existente é `LocalDate`: uma bolsa que vence hoje é elegível; horário de vencimento e prazo de entrega não fazem parte deste modelo.
- A consulta não cria uma reserva provisória. A confirmação recebe os códigos exibidos e revalida sua elegibilidade; não troca silenciosamente uma bolsa por outra.
- Só é confirmada a quantidade total solicitada. Uma proposta incompleta informa a falta e não pode ser confirmada parcialmente.
- O HTML continua sendo o protótipo estático. A implementação desta etapa é do backend, demonstrável por Postman ou cliente HTTP.
- Não inclui cancelamento, expedição, autenticação ou roteirização.

## Roteiro de execução pelo Postman

Inicie o PostgreSQL e crie o banco `rotavital`. Configure `DB_PASSWORD` e execute `mvn spring-boot:run`, como descrito no README principal.

Use `Content-Type: application/json` nos POSTs. Nos exemplos de bolsas, use datas futuras em relação ao dia da execução.

### 1. Cadastrar duas bolsas usando a API existente

`POST http://localhost:8080/api/v1/bolsas`

```json
{
  "codigo": "E3-B101",
  "componente": "CONCENTRADO_HEMACIAS",
  "tipoSanguineo": "A_POSITIVO",
  "validade": "2026-11-01",
  "status": "DISPONIVEL"
}
```

Repita com código `E3-B102` e validade `2026-11-02`. Para evidenciar a filtragem, cadastre também `E3-B104`, tipo `B_POSITIVO`, validade `2026-10-31`.

### 2. Criar a requisição

`POST http://localhost:8080/api/v1/requisicoes`

```json
{
  "hospital": "Hospital Aurora",
  "componente": "CONCENTRADO_HEMACIAS",
  "tipoSanguineo": "A_POSITIVO",
  "quantidade": 2
}
```

Retorna 201 e o `id` gerado. Use esse id nos próximos passos; não suponha que seja sempre 1.

### 3. Consultar a proposta

`GET http://localhost:8080/api/v1/requisicoes/{id}/proposta-alocacao`

Com estoque contendo apenas as bolsas acima, retorna E3-B101 e E3-B102, nessa ordem, com `solicitadas: 2`, `selecionadas: 2` e `faltantes: 0`. A B104 é incompatível e fica fora. Outros registros elegíveis já existentes no banco também participam da seleção FEFO.

### 4. Confirmar as bolsas retornadas

`POST http://localhost:8080/api/v1/requisicoes/{id}/reserva`

```json
{"codigosBolsas": ["E3-B101", "E3-B102"]}
```

Retorna 200, status `ALOCADA` e os códigos vinculados. Se a proposta tiver retornado outros códigos, use aqueles códigos.

### 5. Verificar a reserva

- `GET /api/v1/bolsas`: as duas bolsas devem estar RESERVADAS.
- `GET /api/v1/requisicoes/{id}`: a requisição deve estar ALOCADA e ter os dois códigos.
- Repetir a confirmação: deve retornar 409.
- Criar outra requisição e consultar sua proposta: as bolsas reservadas não devem aparecer.
- Reiniciar a aplicação e repetir as consultas para demonstrar persistência no PostgreSQL.

## Testes automatizados

Execute `mvn test`. Os testes usam H2 em modo PostgreSQL, apenas no escopo de teste, sem exigir a senha ou alterar o banco de desenvolvimento.

A suíte cobre FEFO, empate por código, exclusões, faltantes, confirmação, vínculo persistido, entradas inválidas, erros 404/409, proposta desatualizada, vencimento na confirmação e disputa concorrente pela mesma bolsa. Os testes existentes das cinco páginas também permanecem.

A concorrência automatizada usa H2; a validação operacional no PostgreSQL deve ser feita no ambiente da equipe.
