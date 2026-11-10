# Entrega 04 — Histórias Implementadas (Diferenciação @logged, validação com spring security de usuário e password, e telas de rota e monitoramento)

## História 05 — Cadastro e autenticação de usuários

> **Como** usuário do Rota Vital  
> **Quero** criar uma conta e realizar login no sistema  
> **Para** acessar de forma segura as funcionalidades disponíveis para usuários autenticados.

### Critérios de aceitação

- O usuário deve conseguir criar uma conta informando nome, e-mail e senha.
- O sistema deve impedir o cadastro de e-mails duplicados.
- A senha deve ser armazenada de forma segura utilizando BCrypt.
- O usuário deve conseguir realizar login utilizando suas credenciais.
- Usuários não autenticados não devem acessar páginas e APIs protegidas.
- O usuário autenticado deve conseguir encerrar sua sessão através da opção de logout.

### Funcionalidades relacionadas

- `GET /registro`
- `POST /registro`
- `GET /login`
- `POST /login`
- `POST /logout`

---

## História 06 — Utilização dos fluxos do sistema pela interface web

> **Como** usuário autenticado do Rota Vital  
> **Quero** consultar e executar as operações de estoque, requisição e alocação através da interface web  
> **Para** utilizar as funcionalidades do sistema sem depender diretamente das chamadas da API REST.

### Critérios de aceitação

- O usuário autenticado deve conseguir consultar as bolsas cadastradas no estoque.
- O usuário deve conseguir cadastrar novas bolsas através da interface.
- O sistema deve permitir o registro de uma nova requisição de hemocomponentes.
- A requisição criada deve ser vinculada ao usuário autenticado.
- O usuário deve conseguir consultar a proposta de alocação de sua requisição.
- O usuário deve conseguir confirmar a reserva das bolsas selecionadas.
- Um usuário não deve conseguir acessar requisições pertencentes a outra conta.
- As telas devem utilizar os dados persistidos pelo backend.

### Funcionalidades relacionadas

- `GET /estoque`
- `POST /bolsas`
- `GET /requisicao`
- `POST /requisicoes`
- `GET /alocacao?requisicaoId={id}`
- `POST /alocacao/{id}/reserva`

Telas 04 e 05 dos protótipos do figma — Rota, monitoramento e identidade visual

Nesta entrega as telas 04 (Rota) e 05 (Monitoramento), que eram demonstrações estáticas,
passam a usar dados persistidos pelo backend. O monitoramento é ligado à alocação: assim que
a reserva é confirmada, a remessa é aberta e o sensor da caixa térmica começa a ser monitorado.
Todas as telas receberam a identidade vermelho e branco, com o logo no canto superior esquerdo.

## História 07 — Monitorar a cadeia fria da remessa

> **Como** responsável pela distribuição  
> **Quero** que o sensor da caixa térmica seja monitorado a partir da confirmação da reserva  
> **Para** identificar desvios de temperatura e retirar de uso as bolsas afetadas.

### Critérios de aceitação

- Ao confirmar a reserva, uma remessa é aberta na mesma transação, com a caixa térmica
  dimensionada pela quantidade de bolsas e a faixa de temperatura do componente. O
  monitoramento começa nesse momento.
- Se a abertura da remessa falhar, a reserva inteira é desfeita. Cada requisição tem no
  máximo uma remessa. Reservas feitas antes desta entrega podem abrir a remessa pela tela de
  alocação (**Iniciar monitoramento**).
- Cada leitura guarda horário, temperatura e conectividade e é classificada como *dentro da
  faixa*, *início do desvio* (primeira leitura fora da faixa) ou *alerta térmico* (segunda
  leitura consecutiva fora da faixa).
- O alerta bloqueia as bolsas no estoque (situação **Indisponível**) e deixa a remessa
  **Pendente de avaliação**. Voltar à faixa encerra o desvio, mas não desfaz o alerta.
- Reconhecer o alerta registra quem e quando, sem liberar as bolsas. Reconhecer sem alerta ou
  pela segunda vez retorna 409.
- Leitura sem temperatura, fora de −50 a 60 °C ou com conectividade desconhecida retorna 400
  sem registrar nada.
- A entrega encerra o monitoramento; novas leituras retornam 409.
- Somente o dono da requisição consulta ou altera a remessa: 403 para outra conta e 404 para
  remessa inexistente.

### Funcionalidades relacionadas

- `GET /monitoramento?remessaId={id}` e `POST /alocacao/{id}/monitoramento` (reservas antigas)
- `POST /monitoramento/{id}/leituras`, `POST /monitoramento/{id}/alerta/reconhecimento`
- `POST /monitoramento/{id}/transporte`, `POST /monitoramento/{id}/entrega`
- API: `GET /api/v1/remessas`, `GET /api/v1/remessas/{id}`, `POST /api/v1/remessas/{id}/leituras`,
  `POST /api/v1/remessas/{id}/alerta/reconhecimento`, `POST /api/v1/remessas/{id}/transporte`,
  `POST /api/v1/remessas/{id}/entrega`

---

## História 08 — Planejar a rota de entrega

> **Como** planejador logístico  
> **Quero** comparar os caminhos até o hospital e confirmar o mais rápido que seja viável  
> **Para** entregar as bolsas dentro da janela do hospital sem exceder a autonomia térmica da caixa.

### Critérios de aceitação

- A simulação compara até três caminhos da malha sintética (no máximo quatro trechos, sem
  passar por outros hospitais), do mais rápido para o mais lento.
- No horário de pico (07h–09h e 17h–19h, pela hora de saída) as vias urbanas ficam 50% mais
  lentas; as vias expressas não mudam.
- Para cada caminho são calculados chegada, espera pela abertura da janela, atendimento de
  5 minutos e tempo total, e verificados quatro critérios: janela de recebimento, capacidade da
  caixa, autonomia térmica e validade das bolsas na data da entrega.
- A proposta é o caminho viável de menor duração. O planejador pode escolher outra
  alternativa viável.
- A confirmação recalcula tudo no servidor: caminho inviável retorna 409 e caminho fora das
  alternativas retorna 400. Janela invertida, saída no passado ou destino fora da malha
  retornam 400.
- A remessa passa para **Rota planejada** e pode ser replanejada até a saída. Remessa com
  alerta, em transporte ou entregue não pode ser planejada (409).
- O hospital digitado na requisição é reconhecido na malha ignorando maiúsculas, acentos e o
  prefixo "Hospital". Se não for reconhecido, a tela pede o hospital de destino.

### Funcionalidades relacionadas

- `GET /rota?remessaId={id}&destino=&saida=&janelaInicio=&janelaFim=`
- `POST /rota/{id}/planejamento`
- API: `GET /api/v1/remessas/{id}/rota/alternativas`, `POST /api/v1/remessas/{id}/rota`

---

## Frontend — Identidade visual e responsividade

- Logo Rota Vital (gota com pulso) no canto superior esquerdo de todas as telas, inclusive
  login, registro e página de erro, e favicon próprio.
- Tema vermelho e branco com componentes padronizados: cartões, indicadores, etiquetas de
  situação, mensagens de sucesso, erro e aviso.
- A página de erro ganhou a meta tag de viewport que faltava.


---

## Ciclo da remessa

```text
Reserva confirmada ──► AGUARDANDO_ROTA ──planejar──► PLANEJADA ──iniciar transporte──► EM_TRANSPORTE ──confirmar entrega──► ENTREGUE
                              │                          │                                 │
                              └──────────── 2 leituras consecutivas fora da faixa ─────────┴──► PENDENTE_AVALIACAO (bolsas bloqueadas)
```

## Comunicação entre alocação e monitoramento

`AlocacaoService.confirmar` reserva as bolsas, marca a requisição como alocada e publica o
evento `ReservaConfirmada`. `MonitoramentoService.aoConfirmarReserva` escuta esse evento
(`@EventListener`, síncrono) e abre a remessa na mesma transação. A alocação não conhece o
monitoramento (padrão Observer), mas uma falha ao abrir a remessa desfaz a reserva.

## Arquivos centrais para estudar

1. `model/Remessa.java`: abertura, leituras, alerta, plano de rota e ciclo de entrega.
2. `model/LeituraTemperatura.java`, `FaixaTermica`, `ModeloCaixa`, `StatusRemessa`, `CondicaoLeitura`.
3. `model/ReservaConfirmada.java`, `service/AlocacaoService.java` e `service/MonitoramentoService.java`.
4. `service/MalhaViaria.java`: pontos, trechos e busca dos caminhos simples (DFS).
5. `service/ViabilidadeDeRota.java`: regras de viabilidade, sem banco nem Spring.
6. `service/RotaService.java`: simulação, sugestão de parâmetros e confirmação.
7. `controller/RotaTelaController.java`, `MonitoramentoTelaController.java`, `RemessaController.java` (API) e `TelasModel.java`.
8. `templates/rota.html` (mapa em SVG) e `templates/monitoramento.html`.

Os arquivos são relativos a `src/main/java/br/com/rotavital` e `src/main/resources`.

##  Modelagem do Banco de dados

`ddl-auto=update` cria as tabelas `remessa` e `leitura_temperatura`. As chaves estrangeiras
usam `ON DELETE CASCADE` a partir da requisição. Nenhum enum já gravado ganhou valores novos
(o bloqueio usa a situação `INDISPONIVEL` existente), então bancos PostgreSQL existentes
continuam válidos. O Compose define `TZ=America/Recife` para que leituras, saídas e janelas
usem o fuso local.

A primeira execução no Docker, com PostgreSQL 17, levou a três ajustes de desempenho:

- **Índices nas chaves estrangeiras**, que o PostgreSQL não cria sozinho:
  `idx_leitura_remessa_horario (remessa_id, registrada_em)`, `idx_requisicao_usuario (usuario_id)`
  e `idx_requisicao_bolsa_requisicao (requisicao_id)`.
- **Listagens sem N+1:** as listas de remessas e de requisições passaram de 1 + N consultas
  (4 consultas para 3 itens) para 1 consulta.
- **`spring.jpa.open-in-view=false`:** o banco só é consultado dentro dos serviços, e as telas
  recebem os dados prontos.

📄 Detalhes em [Melhorias do backend a partir do log do Docker](melhorias-backend.md).

## Testes e integração com img Docker

- **338 testes, 0 falhas** (258 anteriores + 80 novos): `RemessaDominioTests`,
  `MalhaViariaTests`, `ViabilidadeDeRotaTests`, `MonitoramentoIntegrationTests`,
  `RotaIntegrationTests`, `FluxoLogisticoWebTests` e `ConsultasIntegrationTests`.
- A suíte foi executada com as mesmas bibliotecas do Spring Boot 4.1.1, compiladas com
  `--release 17`. O build do Docker executa `mvn clean verify` com a suíte completa, e só cria a imagem após a suíte de testes passar.

- As telas foram percorridas com a aplicação em execução (H2) e conferidas em desktop, tablet
  e celular.

## Roteiro de demonstração 

1. `docker compose up --build` e abra http://localhost:8080. Crie uma conta e entre.
2. **Estoque:** cadastre B101 (concentrado de hemácias, A+, validade em 3 dias) e B102
   (concentrado de hemácias, O−, validade em 5 dias).
3. **Requisição:** Hospital Aurora, concentrado de hemácias, A+, 2 bolsas.
4. **Alocação:** confirme a reserva. O painel mostra a remessa R-001 aguardando rota.
5. **Rota:** confira a sugestão e o mapa. Mude a saída para 08:00 e simule: no pico, a
   proposta passa a ser o Anel Viário (P1). Confirme o planejamento.
6. **Monitoramento:** registre 4,0 °C (dentro da faixa), 7,0 °C (início do desvio) e 8,0 °C
   (alerta térmico). Reconheça o alerta e veja no estoque as bolsas indisponíveis.
7. **Caminho feliz:** com outra requisição, planeje a rota, registre uma leitura normal,
   inicie o transporte e confirme a entrega.

## Limites

- O sensor é simulado: as leituras são registradas na tela ou enviadas pela API.
- A malha viária e as faixas térmicas são didáticas e não substituem protocolos clínicos.
- O horário de pico é avaliado pela hora de saída, não por trecho percorrido.
- Não há fluxo de avaliação técnica para liberar bolsas bloqueadas nem perfis distintos de usuário.
