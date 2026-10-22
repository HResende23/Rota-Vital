# Refatoração do backend orientada por testes

## Escopo

Análise do backend existente na base `0c97cbf`: controllers, services, entidades,
DTOs, repositórios, configuração e testes. Esta entrega preserva os endpoints,
payloads, códigos HTTP, regras acadêmicas de compatibilidade, modelo de dados e
limites funcionais já implementados. Não implementa as 15 histórias do backlog.

## Processo aplicado ao código existente

1. Restaurar `.mvn/wrapper/maven-wrapper.properties`, ausente no checkout.
   O Wrapper existente falhava antes de iniciar o Maven.
2. Executar a suíte original: **26 testes aprovados**.
3. Escrever testes de caracterização antes de alterar o código de produção.
   Executar a suíte ampliada: **249 testes aprovados**.
4. Refatorar `AlocacaoService` extraindo seleção FEFO, busca e validação da
   seleção e verificação de elegibilidade para métodos privados.
5. Executar `clean verify`: **249 testes, zero falhas, zero erros, zero ignorados**;
   compilação, testes e empacotamento do JAR concluídos.

Como as funcionalidades já existiam, a abordagem é testes de caracterização
seguidos de refatoração protegida pela suíte. Não se atribui retrospectivamente
um ciclo RED/GREEN ao desenvolvimento original. Os novos testes confirmam o
comportamento anterior à refatoração; não introduzem requisitos novos.

## Cobertura funcional exercitada

| Suíte | Casos | Comportamentos |
|---|---:|---|
| `AlocacaoIntegrationTests` | 23 | Cadastro e consulta de requisições, validações e limites existentes, proposta FEFO, desempate, faltantes, elegibilidade, reserva persistida, atomicidade, erros 400/404/409 e concorrência |
| `CompatibilidadeMatrizTests` | 192 | Todos os pares de oito tipos sanguíneos para cada um dos três componentes, segundo a regra acadêmica atual |
| `CompatibilidadeServiceTests` | 10 | Exemplos de compatibilidade preexistentes |
| `DominioTests` | 8 | Validade, estados de reserva, realocação proibida, quantidade insuficiente e proteção dos vínculos contra mutação externa |
| `EstoqueIntegrationTests` | 9 | Estoque vazio, cadastro e consulta, persistência, duplicata sem sobrescrita, JSON inválido, campos obrigatórios e vencimento |
| `EstoqueServiceTests` | 2 | Duplicata impedida antes da gravação e propagação de falha de persistência |
| `RotaVitalApplicationTests` | 5 | Inicialização do contexto e acesso às páginas existentes |
| **Total** | **249** | **100% dos testes executados aprovados** |

Os testes de integração exercitam controllers, services, JPA, DTOs e entidades
com H2 em modo PostgreSQL. As reservas concorrentes verificam tanto duas
requisições disputando a mesma bolsa quanto duas confirmações da mesma requisição.
O banco de desenvolvimento não é utilizado nem alterado.

## Refatoração realizada

`AlocacaoService.confirmar` passa a coordenar explicitamente:

1. Bloqueio da requisição e verificação de pendência.
2. Validação da quantidade e duplicidade; busca das bolsas com bloqueio.
3. Validação do lote inteiro antes de qualquer alteração de estado.
4. Reserva das bolsas e alocação da requisição na mesma transação.

Os métodos públicos e suas anotações transacionais foram preservados. A ordem
dos bloqueios continua sendo requisição, seguida de bolsas ordenadas por código.
A seleção da proposta continua filtrando compatibilidade antes de limitar a
quantidade. As demais classes de produção não precisaram de alterações.

## Reproduzir

Windows:

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS:

```sh
sh mvnw clean verify
```

É necessário um JDK compatível com o projeto e acesso ao Maven Central no
primeiro uso. Esta execução foi realizada com Java 26 e Maven 3.9.9, mantendo
`java.version=17` no projeto. Relatórios por suíte: `target/surefire-reports`.

## Limites da validação

- 100% de aprovação da suíte não é uma medição de 100% de cobertura de código.
- A concorrência foi executada no H2; não foi validada em PostgreSQL real.
- A compatibilidade continua sendo a regra didática documentada no projeto.
