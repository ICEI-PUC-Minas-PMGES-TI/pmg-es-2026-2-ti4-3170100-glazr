# Fundação do backend e autenticação do Glazr

## 1. Propósito e escopo

Esta especificação define a primeira entrega executável do Glazr, sistema interno de uma única vidraçaria. A entrega contém um backend HTTP, autenticação por sessão, administração de usuários e perfis de acesso, catálogo fixo de permissões, banco PostgreSQL versionado por Flyway e execução local com Docker Compose e Nginx.

Não há frontend, cadastro público, recuperação ou alteração de senha, autenticação federada, JWT, multi-tenancy, módulos comerciais, mensageria, cache distribuído ou serviços externos. Esses itens não condicionam esta entrega.

## 2. Stack e versões

- Java 21.
- Spring Boot 3.5.16, versão estável mais recente da linha 3.x na data desta especificação.
- Maven Wrapper 3.3.2; o build não depende de Maven global.
- PostgreSQL 17.11 em Alpine 3.24.
- Nginx 1.30.4 em Alpine 3.24, linha stable.
- Spring MVC, Spring Security, Spring Data JPA, Bean Validation, Spring Session JDBC, Flyway, Actuator e PostgreSQL JDBC.
- JUnit 5, Spring Boot Test, Spring Security Test e Testcontainers PostgreSQL/JUnit 5.

Versões de bibliotecas gerenciadas pelo BOM do Spring Boot não são sobrescritas sem necessidade comprovada. Não são usados snapshots, milestones, release candidates ou imagens `latest`.

## 3. Arquitetura e dependências

O backend é um monólito modular organizado por funcionalidade. Cada funcionalidade separa contrato HTTP (`api`), casos de uso e transações (`application`), regras e tipos centrais (`domain`) e persistência/adaptação (`infrastructure`). Controllers validam DTOs e delegam; não consultam repositories. Entidades JPA não cruzam a fronteira HTTP.

Pacotes:

```text
com.glazr
  authentication/{api,application,domain,infrastructure}
  user/{api,application,domain,infrastructure}
  accessprofile/{api,application,domain,infrastructure}
  permission/{api,application,domain,infrastructure}
  shared/{api,domain,exception}
  configuration
```

Transações são abertas na camada de aplicação. Mapeamentos simples são explícitos. O código não usa Lombok, MapStruct, classes genéricas `Utils`/`Helper` ou serviços que misturem responsabilidades.

## 4. Modelo relacional

Todos os identificadores de usuário e perfil são UUID gerados pela aplicação. Datas são `Instant` no Java e `timestamptz` no PostgreSQL. Hibernate usa `ddl-auto=validate`; somente Flyway altera o schema.

### 4.1 Usuários

`users` possui `id`, `email`, `display_name`, `password_hash`, `active`, `failed_login_attempts`, `locked_until`, `last_login_at`, `created_at`, `updated_at` e `version`. E-mail é normalizado com `strip()` e minúsculas usando `Locale.ROOT` antes da validação e persistência. A senha nunca sofre trim ou normalização.

O banco impõe unicidade case-insensitive por índice único em `lower(email)`. Usuários não são removidos fisicamente. Um usuário possui um ou mais perfis por `user_access_profiles`, cuja chave composta impede duplicidade.

### 4.2 Perfis e permissões

`access_profiles` possui `id`, `name`, `description`, `active`, `system_profile`, `created_at`, `updated_at` e `version`. O nome é normalizado com `strip()`, e a unicidade case-insensitive é garantida por índice único em `lower(name)`.

`permissions` usa `key` como chave natural, além de `module` e `description`. O catálogo é somente leitura e contém:

- `USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_STATUS_UPDATE`;
- `ACCESS_PROFILE_READ`, `ACCESS_PROFILE_CREATE`, `ACCESS_PROFILE_UPDATE`, `ACCESS_PROFILE_DELETE`, `ACCESS_PROFILE_ASSIGN`;
- `PERMISSION_READ`.

`access_profile_permissions` associa perfis e permissões com chave composta. Chaves desconhecidas são rejeitadas.

O perfil de sistema `ADMINISTRADOR` é criado pela migration, recebe todo o catálogo e não pode ser renomeado, desativado, excluído nem ter permissões removidas por API. Perfis em uso não podem ser excluídos. Toda alteração de usuários preserva ao menos um usuário ativo associado ao perfil `ADMINISTRADOR`.

### 4.3 Sessões

As tabelas oficiais `SPRING_SESSION` e `SPRING_SESSION_ATTRIBUTES`, incluindo índices por expiração e principal, são criadas por migration Flyway. A inicialização automática do schema pelo Spring Session fica desabilitada para que não concorra com Flyway.

## 5. Bootstrap inicial

Depois das migrations, um inicializador transacional verifica a quantidade de usuários. Se houver ao menos um, ignora de forma segura todas as credenciais de bootstrap. Se não houver nenhum, exige simultaneamente:

- `GLAZR_BOOTSTRAP_ADMIN_EMAIL`;
- `GLAZR_BOOTSTRAP_ADMIN_PASSWORD`;
- `GLAZR_BOOTSTRAP_ADMIN_NAME`.

Ausência ou incompletude encerra a inicialização com mensagem que cita apenas os nomes das variáveis. Com dados completos, o serviço valida e normaliza e-mail/nome, valida senha de 12 a 128 caracteres, garante que `ADMINISTRADOR` contém todas as permissões, gera hash versionado com SHA-256/Base64 antes de BCrypt força 12 e cria a primeira conta ativa. Esse pré-hash preserva toda a entrada antes do limite de 72 bytes do BCrypt. A senha e o hash nunca são registrados. As variáveis só são materializadas quando o banco está vazio. O procedimento é idempotente por transação, lock pessimista compartilhado e constraints do banco. As variáveis, especialmente a senha, devem ser removidas do ambiente após a primeira inicialização.

## 6. Autenticação, bloqueio e sessão

`POST /api/v1/auth/login` recebe JSON com e-mail e senha. Um caso de uso normaliza apenas o e-mail e executa uma comparação SHA-256/Base64 + BCrypt inclusive para e-mail desconhecido, reduzindo diferenças observáveis. Depois da comparação cara, uma transação curta relê e bloqueia pessimisticamente a conta pelo UUID observado, confirma que e-mail/hash continuam iguais e então revalida estado e atualiza os contadores atomicamente. E-mail desconhecido, identidade alterada durante a comparação, senha incorreta, conta inativa ou bloqueada produz o mesmo `401`, código `AUTHENTICATION_FAILED` e detalhe genérico.

Cada senha inválida incrementa `failed_login_attempts`. Na quinta falha consecutiva, `locked_until` recebe `agora + 15 minutos`. Enquanto bloqueada, a resposta continua genérica. Após o prazo, uma nova tentativa válida autentica, zera contador/bloqueio e grava `last_login_at`; uma nova tentativa inválida inicia nova sequência. Um `Clock` injetável torna o comportamento determinístico em testes.

Após autenticar, o controller troca o identificador da sessão existente (`changeSessionId`), grava uma autenticação segura no `SecurityContext` e salva o contexto no `HttpSessionSecurityContextRepository`. A resposta contém apenas a representação segura do usuário, nunca o ID da sessão. `POST /logout` invalida a sessão e limpa o cookie.

Spring Session JDBC persiste sessões no PostgreSQL. O cookie chama-se `GLAZR_SESSION`, usa `HttpOnly`, `Path=/`, `SameSite=Lax` e `Secure=true` em produção; em desenvolvimento/teste o atributo Secure é configurável para permitir HTTP local. O timeout padrão é oito horas e pode ser alterado por `GLAZR_SESSION_TIMEOUT` em formato de duração (`8h`, por exemplo).

Um filtro posterior à carga do contexto consulta o usuário de cada sessão autenticada, rejeita e invalida a sessão caso a conta esteja ausente/inativa e recompõe as authorities com as permissões efetivas atuais. A desativação também exclui as sessões encontradas por principal via Spring Session. Assim, desativação e mudanças de perfil têm efeito confiável sem esperar expiração.

## 7. CSRF e CORS

CSRF permanece habilitado com `CookieCsrfTokenRepository`. O cookie `XSRF-TOKEN` é legível pela SPA e o header esperado é `X-XSRF-TOKEN`. `GET /api/v1/auth/csrf` materializa o token e retorna somente `headerName`, `parameterName` e `token`; não retorna sessão ou credencial de autenticação.

Sequência da SPA:

1. `GET /api/v1/auth/csrf`, preservando cookies.
2. `POST /api/v1/auth/login` com cookie e header `X-XSRF-TOKEN`.
3. Preservar o cookie `GLAZR_SESSION` e enviar o header CSRF em toda chamada mutável.
4. Após login, buscar novamente `/auth/csrf` se o token tiver sido rotacionado.
5. `POST /api/v1/auth/logout` com CSRF.

Produção usa mesma origem pelo Nginx e nenhuma origem CORS por padrão. Em desenvolvimento, `GLAZR_ALLOWED_ORIGINS` aceita lista separada por vírgulas. Credenciais são permitidas apenas para origens explícitas; curinga é rejeitado pela configuração.

## 8. Contrato HTTP

Todos os endpoints usam `/api/v1`, JSON camelCase e datas ISO 8601 UTC.

### 8.1 Autenticação

- `GET /auth/csrf`: público; inicializa e apresenta dados do token CSRF.
- `POST /auth/login`: público, mas protegido por CSRF; cria sessão e retorna o mesmo formato de `/auth/me`.
- `POST /auth/logout`: autenticado e protegido por CSRF; responde `204`.
- `GET /auth/me`: autenticado; retorna `id`, `email`, `displayName`, `active`, perfis ativos e permissões efetivas ordenadas.

### 8.2 Usuários

- `POST /users`: `USER_CREATE`; exige e-mail, nome, senha inicial e IDs de perfis ativos não vazios. Se quem chama não possuir `ACCESS_PROFILE_ASSIGN`, responde `403`.
- `GET /users`: `USER_READ`; paginação Spring (`page`, `size`, `sort`), filtro textual opcional por nome/e-mail e `active` opcional.
- `GET /users/{id}`: `USER_READ`.
- `PATCH /users/{id}`: `USER_UPDATE`; altera campos presentes entre e-mail, nome e perfis. Mudança de perfis também exige `ACCESS_PROFILE_ASSIGN`. Não aceita senha.
- `PATCH /users/{id}/status`: `USER_STATUS_UPDATE`; recebe `active` e preserva o último administrador.

Não existe `DELETE /users` nem endpoint público de criação.

### 8.3 Perfis e permissões

- `POST /access-profiles`: `ACCESS_PROFILE_CREATE`.
- `GET /access-profiles`: `ACCESS_PROFILE_READ`, paginado.
- `GET /access-profiles/{id}`: `ACCESS_PROFILE_READ`.
- `PUT /access-profiles/{id}`: `ACCESS_PROFILE_UPDATE`; substitui nome, descrição, status e conjunto de permissões, respeitando proteção do perfil de sistema.
- `DELETE /access-profiles/{id}`: `ACCESS_PROFILE_DELETE`; somente perfil não sistêmico e sem usuários, responde `409` se estiver em uso.
- `GET /permissions`: `PERMISSION_READ`; lista o catálogo ordenado por módulo e chave.

## 9. Erros e logging

Erros seguem `application/problem+json` com `type`, `title`, `status`, `detail`, `instance`, `code` e, em validações, `fieldErrors`. `timestamp` é emitido de modo uniforme. Não há stack trace, SQL, classe interna, senha, hash, cookie ou token em respostas.

Mapeamento mínimo:

- `400`: JSON/entrada inválida (`VALIDATION_ERROR`, `MALFORMED_REQUEST`).
- `401`: sessão ausente/inválida ou login genérico (`AUTHENTICATION_REQUIRED`, `AUTHENTICATION_FAILED`).
- `403`: permissão/CSRF insuficiente (`ACCESS_DENIED`, `INVALID_CSRF_TOKEN`).
- `404`: entidade não encontrada (`RESOURCE_NOT_FOUND`).
- `409`: duplicidade ou regra de domínio (`DUPLICATE_EMAIL`, `DUPLICATE_ACCESS_PROFILE`, `ACCESS_PROFILE_IN_USE`, `LAST_ACTIVE_ADMINISTRATOR`).
- `500`: detalhe genérico (`INTERNAL_ERROR`).

Logs registram contexto técnico mínimo e identificadores internos quando úteis, mas nunca credenciais completas. O login não distingue contas existentes de inexistentes.

## 10. Migrações

- `V1__create_identity_schema.sql`: usuários, perfis, permissões e associações com constraints e índices.
- `V2__create_spring_session_schema.sql`: tabelas e índices oficiais de Spring Session JDBC para PostgreSQL.
- `V3__seed_permission_catalog.sql`: catálogo idempotente por chave, perfil `ADMINISTRADOR` e associação de todas as permissões.

Migrações aplicadas em ambientes compartilhados são imutáveis; mudanças futuras recebem novo número. Nenhuma migration contém senha.

## 11. Infraestrutura e configuração

O `compose.yaml` contém `postgres`, `backend` e `nginx` em rede interna. PostgreSQL usa volume nomeado e healthcheck `pg_isready`, sem porta publicada por padrão; um profile/override de desenvolvimento pode publicar porta configurável. O backend aguarda o healthcheck e também usa o comportamento normal de conexão do pool. Nginx é a única entrada padrão, na porta `${GLAZR_HTTP_PORT:-8080}`.

O Dockerfile é multi-stage: compila com Maven Wrapper em JDK 21 e executa o jar em JRE 21 como usuário não root. Só o jar entra no runtime. `JAVA_OPTS` permite ajuste externo. O healthcheck usa `/actuator/health`; apenas `health` é exposto, sem detalhes.

Nginx remove o header de versão, oferece `/health`, encaminha `/api/` e preserva `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` e cookies. A raiz fica preparada para arquivos estáticos futuros, sem frontend nesta entrega.

Configurações:

- `dev`: cookie sem Secure por padrão, CORS explícito configurável e logs úteis.
- `test`: datasource fornecido por Testcontainers, cookie sem Secure e bootstrap controlado pelo teste.
- `prod`: cookie Secure, sem CORS por padrão, nenhuma credencial default e logs restritos.

## 12. Estratégia de testes e aceite

Testes unitários cobrem normalização, política de senha, criação/atribuição, perfil protegido, último administrador e relógio de bloqueio. Integrações usam PostgreSQL Testcontainers para migrations, unicidade case-insensitive, associações, bootstrap, repositories e sessão. Testes MockMvc com o mesmo PostgreSQL cobrem login, respostas indistinguíveis, inatividade/bloqueio, logout, `/me`, CSRF, authorities e APIs administrativas.

O aceite exige `./mvnw clean verify`, `docker compose config`, build e subida dos três contêineres, saúde, smoke test completo pelo Nginx, inspeção de logs e revisão de segredos/diff. Se o daemon Docker estiver indisponível, o resultado deve ser informado como bloqueio real; não se simula sucesso.

## 13. Evolução futura

Frontend React/TypeScript e redefinição de senha permanecem explicitamente fora desta entrega. Também podem ser avaliadas futuramente auditoria funcional, rotação de credenciais e observabilidade externa, sem antecipar infraestrutura agora.
