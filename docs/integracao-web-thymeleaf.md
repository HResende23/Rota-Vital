# Integração web com Thymeleaf e autenticação

Data: 22/10/2026.

Issue: [#6 — Integração web e autenticação](https://github.com/HResende23/Rota-Vital/issues/6).

## Objetivo e escopo

Transformar login, registro, estoque, requisição e alocação em páginas renderizadas
pelo Spring MVC/Thymeleaf e conectadas aos serviços JPA. Preservar o visual de desktop.
Rota e monitoramento continuam demonstrações, protegidas por login e sinalizadas
explicitamente: não existem serviços de rota/entrega para integrar nesta etapa.

## Estrutura

- HTML movido de src/main/resources/static para src/main/resources/templates.
- CSS e JavaScript permanecem em static/css e static/js.
- Fragmento templates/fragments/layout.html compartilha cabeçalho, navegação,
  identificação do usuário e formulário de logout.
- SecurityConfig configura Spring Security, sessão, BCrypt, login e logout.
- AcessoController atende as páginas públicas e o registro.
- TelasController conecta as cinco telas aos serviços existentes.
- SessaoModel fornece o estado autenticado ao cabeçalho.
- UsuarioDaSessao resolve o usuário conectado e verifica propriedade de requisições.
- RegistroForm, RequisicaoForm e NovaBolsaForm validam entradas no servidor.

## Visitante e usuário conectado

Visitantes acessam /login e /registro. A raiz / redireciona para /login; para uma
sessão autenticada redireciona para /estoque. As telas operacionais e APIs exigem
autenticação. APIs sem sessão retornam HTTP 401, enquanto páginas redirecionam ao login. URLs antigas *.html continuam atendidas por controllers, sem
duplicatas públicas na pasta static.

O cabeçalho público oferece Entrar e Criar conta. O cabeçalho autenticado mostra
o e-mail e Sair; a barra lateral mostra o nome do cadastro. Não há um arquivo
logged.html separado: o estado vem da sessão, sem duplicar o layout inteiro.

## Registro, login e logout

POST /registro recebe nome, e-mail, senha e confirmação. Nome/e-mail são validados,
e-mail é normalizado para minúsculas, duplicatas são rejeitadas. Senha exige de
8 a 72 caracteres e até 72 bytes UTF-8 (limite do BCrypt); confirmação deve coincidir.
Erros mantêm nome/e-mail na tela, sem preencher senhas. Sucesso redireciona ao login.

User recebe senhaHash. O banco armazena somente BCrypt, nunca a senha original.
A coluna aceita nulo para preservar usuários antigos; esses usuários não podem
entrar até existir um fluxo próprio de definição de senha. Não foi criada senha
padrão nem convertida uma conta antiga automaticamente. Não há recuperação de senha.

POST /login é processado pelo Spring Security. Credenciais inválidas geram mensagem
genérica. Sucesso cria/renova a sessão e abre /estoque. POST /logout invalida a
sessão e remove o cookie JSESSIONID. Formulários POST usam token CSRF automático
gerado pelo Thymeleaf. Não foi desabilitada a proteção CSRF nas APIs.

O cadastro REST anterior POST /api/v1/usuarios foi preservado, agora protegido
por login; ele continua sendo cadastro básico sem senha. Para criar uma conta
capaz de autenticar, use /registro. GET de usuário permite somente o próprio cadastro.

## Ações reais nas telas

| Tela | Ação | Integração |
|---|---|---|
| Registro | Criar conta | Validação, hash BCrypt e persistência JPA |
| Login | Entrar | Autenticação Spring Security e sessão |
| Cabeçalho | Sair | Logout por POST com CSRF |
| Estoque | Consultar saldo / Limpar filtros | Consulta real por componente e tipo |
| Estoque | Cadastrar bolsa | Validação e cadastro DISPONIVEL pelo EstoqueService |
| Estoque | Nova requisição | Navegação ao formulário autenticado |
| Requisição | Enviar requisição | Cadastro JPA ligado ao usuário da sessão |
| Requisição | Consultar alocação | Lista apenas requisições do usuário conectado |
| Alocação | Consultar proposta | Compatibilidade e ordenação FEFO existentes |
| Alocação | Confirmar reserva | Revalidação, bloqueios e reserva transacional existentes |

Estoque mostra dados reais e estado vazio, sem bolsas sintéticas fixas. Contadores
mostram totais do estoque, enquanto os filtros afetam a tabela. Bolsas vencidas
são identificadas na tabela. O formulário de bolsas não permite escolher um estado
reservado manualmente. As datas/janelas fictícias da requisição foram retiradas:
não existem campos equivalentes no backend atual.

Em alocação, as bolsas sugeridas são selecionadas por padrão. O servidor valida
quantidade, elegibilidade e concorrência na confirmação. Estoque insuficiente
desabilita a confirmação na tela. Uma reserva concluída mostra os códigos salvos.

## Propriedade das requisições

O formulário web nunca aceita usuarioId como fonte de identidade: obtém o usuário
da sessão. A API existente ainda recebe usuarioId para manter o contrato, mas
rejeita IDs diferentes do usuário autenticado. Consulta, proposta e reserva
verificam a propriedade antes de executar a operação. Requisições antigas sem
usuário não aparecem em Minhas requisições e não são atribuídas automaticamente.

Todos os usuários novos recebem o mesmo papel USUARIO. Não foi implementada uma
matriz de perfis de hospital/hemocentro/administrador. Nesta etapa o usuário pode
cadastrar estoque e reservar apenas suas próprias requisições.

## Banco e execução

A configuração existente ddl-auto=update adiciona senha_hash a usuarios ao iniciar.
Não houve alteração de credenciais do PostgreSQL, limpeza de dados ou execução de
DDL manual. Testes usam H2; validação de PostgreSQL existente deve ser documentada
se executada. Reinicie/recompile a aplicação para disponibilizar templates e
dependências novas. Com Docker, reconstrua a imagem com docker compose up --build.

## Verificação

Execução final: `./mvnw.cmd test -q` — **258 testes, 0 falhas e 0 erros**, em H2.
A suíte anterior foi adaptada para autenticação/CSRF sem desabilitar os filtros.
O starter de testes de segurança específico do Spring Boot 4 foi incluído.

FluxoWebIntegrationTests verifica:
- páginas públicas e redirecionamento de páginas protegidas;
- APIs sem sessão retornando 401 e POST sem CSRF retornando 403;
- registro persistido com hash, login por e-mail normalizado e logout;
- cadastro e filtro de bolsas pela interface MVC;
- requisição vinculada à sessão mesmo quando o cliente envia outro usuarioId;
- proposta, reserva e estado final persistido;
- bloqueio de acesso/reserva de requisição de outra conta;
- senha incorreta, e-mail duplicado, confirmação diferente e senha curta;
- impossibilidade de login em cadastro antigo sem hash.

A revisão no navegador confirmou cadastro real e login no ambiente temporário H2,
cabeçalho público/autenticado, identificação do usuário e estoque vazio.

### Docker e PostgreSQL: validação manual do usuário

O Docker Desktop e somente o serviço db foram iniciados. O banco ficou saudável.
Antes de qualquer atualização da aplicação foi gerado um backup PostgreSQL no
diretório de trabalho desta conversa (antes-thymeleaf.dump). Não houve limpeza
de dados nem alteração manual do schema.

A aplicação Docker **não foi reconstruída nem iniciada** nesta etapa, a pedido do
usuário. Portanto, a migração de senha_hash e a integração da versão nova no
PostgreSQL **ainda não foram validadas**. A prévia em 8082 usa H2 em memória e
não representa os dados persistentes do Docker.

### Roteiro manual

Na pasta C:\Users\rodri\Downloads\Rota-Vital, execute:

```bash
docker compose up --build
```

1. Abra http://localhost:8080/ e confirme que aparece login.
2. Clique em Criar conta, registre nome/e-mail/senha e entre.
3. Confira nome/e-mail, navegação autenticada e botão Sair.
4. Cadastre uma bolsa disponível com validade futura e filtre o estoque.
5. Crie uma requisição compatível para uma bolsa e abra a proposta.
6. Confirme a reserva e confira o status Alocada e a bolsa Reservada.
7. Saia e confirme que as páginas operacionais exigem login.
8. Entre em outra conta e confira que as requisições anteriores não aparecem.
9. Teste senha incorreta, e-mail duplicado, senhas diferentes e estoque insuficiente.
10. Reinicie os containers e confira a persistência dos dados.

Nenhum commit ou push desta implementação foi feito. A issue #6 permanece aberta,
aguardando a validação manual e a publicação que o usuário decidir realizar.

## Limites

Sem recuperação de senha, verificação de e-mail, perfis distintos ou backend de
rotas/monitoramento. Sem commit/push automático nesta implementação.

## Revisão de interface

Os enums mantêm seus valores técnicos na API e passam a expor rótulos legíveis
somente nos templates (por exemplo, A+ e Concentrado de hemácias). Uma página de
erro compartilhada trata navegação inválida/permissão negada sem expor detalhes
internos. Não foram adicionadas novas regras adaptativas ao celular; mantiveram-se
os estilos previamente existentes das cinco telas.

## Arquivos centrais para estudar

1. model/User.java: entidade, construtores e hash persistido.
2. config/SecurityConfig.java: proteção, BCrypt, login, logout e sessão.
3. controller/AcessoController.java: entrada do cadastro e tratamento de validação.
4. controller/TelasController.java: dados das telas e ações dos formulários.
5. controller/UsuarioDaSessao.java: identidade e verificação da propriedade.
6. service/UserService.java: cadastro básico e registro com hash.
7. service/AlocacaoService.java: regras existentes e listagem por usuário.
8. templates/fragments/layout.html: layout público/autenticado.
9. FluxoWebIntegrationTests.java: exemplo executável do caminho de ponta a ponta.

Os arquivos são relativos a src/main/java/br/com/rotavital, src/main/resources
e src/test/java/br/com/rotavital conforme seu tipo. Esta implementação não cria
uma segunda página HTML duplicada para representar o estado logged.
