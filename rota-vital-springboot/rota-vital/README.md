# Rota Vital

Primeiro incremento: projeto Spring Boot com cinco páginas HTML e CSS de baixa fidelidade, baseados no prototipo do figma(https://www.figma.com/proto/CZ4BYx3COy4SKGpxZ36Zu1/Rota-Vital-%E2%80%94-Entrega-01?node-id=1-72&starting-point-node-id=1%3A2).

## Executar no VS Code

Requisitos: JDK 17 ou superior compatível (recomendado JDK 21 ou 25), variável JAVA_HOME configurada e acesso à internet no primeiro uso. Instale as extensões Extension Pack for Java e Spring Boot Extension Pack no VS Code.

1. Extraia o projeto e abra a pasta que contém `pom.xml` no VS Code.
2. Aguarde a importação do projeto Maven.
3. No terminal PowerShell do Windows, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```sh
./mvnw spring-boot:run
```

4. Abra <http://localhost:8080/>. Para encerrar, pressione Ctrl+C no terminal.

O Maven Wrapper fornece a versão do Maven usada pelo projeto; não é necessário instalar Maven separadamente. Também é possível executar `RotaVitalApplication` pelo VS Code.

## Telas

| Página | Arquivo em `src/main/resources/static/` |
| --- | --- |
| Estoque | `index.html` |
| Requisição hospitalar | `requisicao.html` |
| Alocação de bolsas | `alocacao.html` |
| Planejamento de rota | `rota.html` |
| Monitoramento da remessa | `monitoramento.html` |

O CSS compartilhado fica em `src/main/resources/static/css/styles.css`.

As páginas são HTML estático servido pelo Spring Boot. Não há Thymeleaf ou controladores: a pasta `templates` exigiria um mecanismo de renderização desnecessário neste incremento. É possível editar o HTML e o CSS diretamente e abri-los no navegador para revisão visual.

## Escopo do primeiro commit

- Spring Boot 4.1.1, Java 17 e Maven Wrapper gerado pelo Spring Initializr.
- Cinco telas em tons de cinza, com menu lateral e destaque da página atual.
- Links de avanço entre requisição, alocação, rota e monitoramento.
- Campos somente leitura e dados sintéticos fixos. Os botões de avanço não enviam, reservam ou salvam dados.
- Consultar saldo e Reconhecer alerta são botões ilustrativos desabilitados.
- HTML semântico, navegação por teclado, link para pular ao conteúdo e adaptações para telas menores.
- Sem JavaScript, API de negócio, banco, pipeline ou contêiner.

A aplicação local escuta somente em `127.0.0.1`. Não foi implementada autenticação no Spring Boot. A revisão hospedada, quando disponível, usa o controle de acesso privado da plataforma de hospedagem e contém os mesmos arquivos estáticos.

## Verificação

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

No Linux/macOS, use `./mvnw` no lugar de `.\mvnw.cmd`.

O teste parametrizado verifica que o Spring serve as cinco páginas como HTML. Nesta entrega, a análise dos arquivos confirmou a estrutura HTML, os links locais, a página ativa no menu e os campos somente leitura. A execução Maven neste ambiente foi bloqueada pela resolução de DNS do Maven Central; portanto, compilação e testes Spring ainda precisam ser executados na máquina de desenvolvimento. Não houve teste visual automatizado em navegador.

## Referências técnicas

- [Requisitos oficiais do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Initializr](https://start.spring.io/)

## Git

O commit inicial usa a mensagem `feat: initialize Spring Boot with five low-fidelity wireframes`. Arquivos gerados, configurações locais de IDE e segredos não devem ser versionados. O envio remoto depende de um repositório de destino com acesso de escrita.
