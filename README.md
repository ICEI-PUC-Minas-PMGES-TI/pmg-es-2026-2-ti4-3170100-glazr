# Glazr

O **Glazr** é um sistema web integrado para gestão de vidraçarias, projetado para otimizar e automatizar o controle operacional e comercial do negócio. A plataforma centraliza o catálogo de produtos e acabamentos de vidros, gestão de estoque e chapas, controle de perfis e acessos (Administradores e Vendedores), cálculo de orçamentos e acompanhamento de pedidos de forma ágil, intuitiva e segura.


## Alunos integrantes da equipe

* Caio Alves Kfuri
* Guilherme Horta Dias
* João Heleno
* Lucas Batista Duarte
* Murilo Freitas de Souza
* Rafael Abras Lessa Freitas


## Professores responsáveis

* Filipe Torio Lopes Ruas Nhimi
* Lucca Soares de Paiva Lacerda

## Tecnologias Utilizadas

O projeto adota uma arquitetura moderna dividida entre uma Single Page Application (SPA), uma API RESTful corporativa, banco relacional e infraestrutura containerizada:

### Frontend
- **React 19** e **TypeScript** (modo estrito)
- **Vite 8** como bundler e ambiente de desenvolvimento ágil
- **React Router** para navegação e proteção de rotas
- **Tailwind CSS v4** para estilização
- **Framer Motion** e **GSAP** para microinterações e transições fluidas
- **Lucide React** para ícones
- **Vitest**, **Testing Library** e **MSW (Mock Service Worker)** para testes de componentes e contratos HTTP

### Backend
- **Java 21** e **Spring Boot 3.5.16**
- **Spring Security** (autenticação por sessão com cookies `HttpOnly`, proteção CSRF e hash de senha com BCrypt força 12)
- **OAuth2 Client & Google OpenID Connect (OIDC)** para autenticação federada opcional
- **Spring Session JDBC** para persistência e compartilhamento de sessões
- **Spring Data JPA / Hibernate** para persistência de dados
- **Flyway** para versionamento e migrações automatizadas do banco de dados
- **Spring Mail** para envio de convites e recuperação de senhas
- **Testcontainers** para testes de integração com instâncias reais do PostgreSQL

### Banco de Dados & Infraestrutura
- **PostgreSQL 17** (imagem Alpine) como banco de dados relacional
- **Nginx** como proxy reverso unificador de origens e servidor de arquivos estáticos da SPA
- **Mailpit** para interceptação e visualização de e-mails em ambiente de desenvolvimento local
- **Docker & Docker Compose** para orquestração simplificada de todos os serviços

---

## Instruções de Utilização

A forma mais rápida e recomendada de executar a aplicação completa é utilizando o **Docker Compose**, pois ele provisiona e conecta automaticamente o banco de dados, o backend, o frontend e o servidor de e-mails local.

### 1. Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) ou Docker Engine com Docker Compose v2 instalado e em execução no sistema.
- *(Opcional para desenvolvimento fora de contêineres)*: **Java 21**, **Node.js (>= 22)** e **PostgreSQL 17**.

---

### 2. Executando com Docker Compose (Recomendado)

#### Passo 1: Acessar a pasta do código
Abra o terminal na raiz do repositório e entre na pasta `Codigo`:

```bash
cd Codigo
```

#### Passo 2: Configurar as variáveis de ambiente
Crie o arquivo `.env` a partir do modelo `.env.example`:

- **Linux / macOS (Bash):**
  ```bash
  cp .env.example .env
  ```
- **Windows (PowerShell):**
  ```powershell
  Copy-Item .env.example .env
  ```

> [!NOTE]
> O arquivo `.env` já contém valores padrão adequados para desenvolvimento local. Você pode ajustar as variáveis de bootstrap do primeiro administrador (`GLAZR_BOOTSTRAP_ADMIN_EMAIL`, `GLAZR_BOOTSTRAP_ADMIN_PASSWORD` e `GLAZR_BOOTSTRAP_ADMIN_NAME`) caso deseje credenciais personalizadas.

#### Passo 3: Construir e iniciar os contêineres
Execute o Docker Compose para construir as imagens e iniciar os serviços em segundo plano:

```bash
docker compose --env-file .env up -d --build
```

Para verificar o status e a saúde dos contêineres:

```bash
docker compose ps
```

---

### 3. Acessando os Serviços

Após a inicialização dos contêineres, os serviços estarão disponíveis nos seguintes endereços:

| Serviço | URL | Descrição |
| :--- | :--- | :--- |
| **Aplicação Web (Glazr)** | [http://localhost:8080](http://localhost:8080) | Interface do usuário e aplicação completa via Nginx |
| **API Backend** | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) | Endpoints REST da aplicação |
| **Mailpit (Webmail)** | [http://localhost:8025](http://localhost:8025) | Interface para leitura dos e-mails de convite e recuperação |

---

### 4. Primeiro Acesso (Bootstrap do Administrador)

Por questões de segurança, a aplicação não possui cadastro público. Na primeira inicialização com o banco de dados vazio, o Glazr cria automaticamente o usuário administrador inicial conforme definido no arquivo `.env`:

- **E-mail padrão:** `admin@example.invalid` (ou o valor em `GLAZR_BOOTSTRAP_ADMIN_EMAIL`)
- **Senha padrão:** `troque-esta-frase-senha-inicial` (ou o valor em `GLAZR_BOOTSTRAP_ADMIN_PASSWORD`)

Acesse [http://localhost:8080/login](http://localhost:8080/login) para entrar no sistema com essas credenciais e gerenciar funcionários, perfis e permissões.

---

### 5. Convites de Usuários e E-mails Locais

1. Como Administrador, acesse a tela de **Administração de Usuários** e cadastre um novo funcionário.
2. Acesse o **Mailpit** em [http://localhost:8025](http://localhost:8025) para visualizar a mensagem com o token de ativação enviado.
3. Abra o link recebido no e-mail para cadastrar a senha e ativar a conta do funcionário.

---

### 6. Parando a Aplicação e Logs

- Para acompanhar os logs em tempo real:
  ```bash
  docker compose logs -f backend
  ```
- Para parar os contêineres sem remover os dados do banco:
  ```bash
  docker compose down
  ```

> [!CAUTION]
> Para resetar a base de dados completamente e apagar os volumes de dados, execute `docker compose down -v`.

---

### 7. Desenvolvimento Local (Opcional)

Caso queira trabalhar no código com recarregamento automático (*hot reload*):

#### Frontend (Vite Dev Server):
1. Com a stack Docker ativa para prover a API:
   ```bash
   cd Codigo/frontend
   npm ci
   npm run dev
   ```
2. Acesse [http://localhost:5173](http://localhost:5173). As requisições para `/api` são redirecionadas automaticamente para o backend (`http://localhost:8080`).

#### Backend (Spring Boot):
1. Inicie o PostgreSQL local ou execute apenas a infraestrutura básica via Docker (`docker compose up -d postgres mailpit`).
2. No diretório `Codigo/backend`:
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```
   - **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```

#### Execução de Testes:
- **Frontend:** no diretório `Codigo/frontend`, execute `npm run test:run` (ou `npm run typecheck` e `npm run lint`).
- **Backend:** no diretório `Codigo/backend`, execute `./mvnw clean verify`.
