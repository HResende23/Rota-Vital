# ⚙️ FlowForge Engine — Motor de Workflow & Automação de Processos

O **FlowForge Engine** é um motor de workflow e automação de processos dinâmico desenvolvido em **Java** e **Spring Boot**. O sistema permite que usuários configurem, executem e rastreiem sequências personalizadas de tarefas e fluxos de aprovação de forma totalmente flexível e programática.

---

## Integrantes do Projeto

* Hilton Resende Montes Neto
* Jardel Simplicio de Oliveira Junior
* Dayanne Cristina Moraes Inacio

## 🚀 Funcionalidades Principais

*   **Configuração Dinâmica:** Criação de fluxos customizados (Ex: *"Se aprovado pelo Gerente, envie para o Diretor; se rejeitado, volte ao Início"*).
*   **Gatilhos Automatizados:** Disparo de ações automáticas baseadas em eventos do ciclo de vida do processo.
*   **Histórico e Auditoria:** Rastreamento completo de cada mudança de estado e decisão tomada dentro de um fluxo.
*   **Arquitetura Extensível:** Pronto para integração com serviços externos de notificação (e-mail, mensageria, etc.).

---

## 🧠 Foco em Programação Orientada a Objetos (POO)

O core do motor foi desenhado utilizando **Design Patterns** clássicos para garantir baixo acoplamento e alta extensibilidade:

*   **State Pattern:** Utilizado para gerenciar de forma isolada os estados de cada `Processo` (ex: *Rascunho*, *Em Validação*, *Aprovado*, *Rejeitado*), eliminando condicionais (`if/else`) complexas.
*   **Chain of Responsibility Pattern:** Aplicado na fila de tarefas e aprovações, permitindo que múltiplos `Avaliadores` processem ou encaminhem a demanda dinamicamente pela cadeia.

---

## 🏗️ Estrutura de Classes Principais

A arquitetura do domínio está dividida nos seguintes componentes chave:

*   **`Processo`**: Entidade principal que carrega o estado atual, o histórico e o contexto do fluxo.
*   **`Tarefa`**: Unidade de trabalho individual a ser executada dentro do processo.
*   **`Gatilho`**: Componente responsável por disparar transições ou ações automáticas.
*   **`Avaliador`**: Representa os nós de decisão ou usuários responsáveis por aprovar/rejeitar etapas.
*   **`Historico`**: Registro imutável de auditoria contendo logs de todas as transições e responsáveis.

---

## 🛠️ Tecnologias Utilizadas

*   **Java** 
*   **Spring Boot 3.x**
*   **Spring Data JPA** (Persistência de dados)
*   **PostgreSQL** / **H2 Database** (Banco de dados)
*   **Maven** (Gerenciador de dependências)

---

## 🏁 Como Executar o Projeto

### Pré-requisitos
*   Java JDK instalado.
*   Maven instalado (opcional, pode usar o `./mvnw` incluso).

### Passos para execução
1. Clone o repositório:
   ```bash
   git clone https://github.com
   ```
2. Acesse a pasta do projeto:
   ```bash
   cd flowforge-engine
   ```
3. Execute a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```

A API estará disponível por padrão em `http://localhost:8080`.

---

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.
