# Entrega 04 — Histórias Implementadas

## História 05 — Cadastro e autenticação de usuários

**Como** usuário do Rota Vital  
**Quero** criar uma conta e realizar login no sistema  
**Para** acessar de forma segura as funcionalidades disponíveis para usuários autenticados.

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

**Como** usuário autenticado do Rota Vital  
**Quero** consultar e executar as operações de estoque, requisição e alocação através da interface web  
**Para** utilizar as funcionalidades do sistema sem depender diretamente das chamadas da API REST.

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

---

As histórias desta entrega representam a integração entre a interface web e as funcionalidades já disponíveis no backend, incluindo autenticação, persistência e controle de acesso.