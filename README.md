# Glazr — fundação do backend

Esta etapa entrega o backend inicial do Glazr, sistema interno de uma única vidraçaria. Inclui autenticação por e-mail e senha, sessões persistidas, usuários, perfis configuráveis, permissões predefinidas, PostgreSQL, Nginx e testes. Não há frontend nem cadastro público.

## Tecnologias

- Java 21 e Spring Boot 3.5.16;
- Maven 3.9.11 por Maven Wrapper 3.3.2;
- PostgreSQL 17.11 e Flyway;
- Spring Security, Spring Session JDBC e SHA-256/Base64 + BCrypt força 12 com formato versionado;
- Testcontainers 1.21.4, gerenciado pelo Spring Boot;
- Docker Compose e Nginx stable 1.30.4.

O build não precisa de Maven, PostgreSQL ou Nginx instalados no host. Para executar fora de contêiner, é necessário um JDK 21 e acesso a PostgreSQL.

## Estrutura

```text
backend/                 aplicação Spring Boot, wrapper, migrations e testes
  src/main/java/com/glazr/
    authentication/      login, principal e revalidação de sessão
    user/                usuários e proteção do último administrador
    accessprofile/       perfis configuráveis e perfil protegido
    permission/          catálogo somente leitura
    shared/              erros HTTP seguros
    configuration/       segurança e bootstrap
docs/                    spec, arquitetura e plano de implementação
infra/nginx/             proxy reverso
scripts/smoke-test.ps1   verificação ponta a ponta
compose.yaml             PostgreSQL, backend e Nginx
```

## Preparar o ambiente

Pré-requisitos mínimos: Docker Desktop com daemon Linux operacional e Docker Compose v2. Para comandos Maven fora do Docker, use Java 21 e confirme com `java -version`.

Copie o exemplo e substitua todos os valores ilustrativos:

```powershell
Copy-Item .env.example .env
```

O `.env` é ignorado pelo Git. Use uma senha longa e exclusiva em `DB_PASSWORD` e uma frase-senha de 12 a 128 caracteres em `GLAZR_BOOTSTRAP_ADMIN_PASSWORD`. `GLAZR_ALLOWED_ORIGINS` aceita origens explícitas separadas por vírgulas; nunca use `*` com credenciais.

## Subir com Docker Compose

```powershell
docker compose --env-file .env config
docker compose --env-file .env build
docker compose --env-file .env up -d --wait
docker compose ps
```

A API fica disponível pelo Nginx em `http://localhost:8080/api/v1` por padrão. A rota `http://localhost:8080/health` verifica o Nginx; o backend expõe somente `/actuator/health` dentro da rede. PostgreSQL não publica porta no host.

Logs e parada sem apagar dados:

```powershell
docker compose logs -f backend
docker compose logs --no-color postgres backend nginx
docker compose down
```

> **Atenção — apaga dados:** `docker compose down -v` remove também o volume `glazr_postgres_data`. Execute esse comando somente quando quiser descartar definitivamente o banco local.

## Bootstrap do primeiro administrador

Quando `users` está vazia, o backend exige simultaneamente `GLAZR_BOOTSTRAP_ADMIN_EMAIL`, `GLAZR_BOOTSTRAP_ADMIN_PASSWORD` e `GLAZR_BOOTSTRAP_ADMIN_NAME`. Ele cria uma conta ativa associada ao perfil protegido `ADMINISTRADOR`, que já possui todas as permissões.

O processo é idempotente: quando existe algum usuário, as variáveis são ignoradas. Após a primeira inicialização, remova `GLAZR_BOOTSTRAP_ADMIN_PASSWORD` e, preferencialmente, todas as variáveis `GLAZR_BOOTSTRAP_ADMIN_*` do ambiente/`.env`, e recrie o contêiner do backend. A senha nunca é registrada em log.

```powershell
docker compose --env-file .env up -d --force-recreate backend
```

## CSRF, login, chamada autenticada e logout

A sessão usa o cookie `GLAZR_SESSION` (`HttpOnly`, `SameSite=Lax`, `Path=/`; `Secure` em produção). CSRF usa um cookie separado `XSRF-TOKEN`, legível pela SPA, e o header `X-XSRF-TOKEN`.

Exemplo PowerShell sem senha literal no histórico:

```powershell
$base = "http://localhost:8080"
$email = Read-Host "E-mail"
$password = Read-Host "Senha" -AsSecureString
$plain = [Net.NetworkCredential]::new("", $password).Password
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod "$base/api/v1/auth/csrf" -WebSession $session
$headers = @{ $csrf.headerName = $csrf.token }
$body = @{ email = $email; password = $plain } | ConvertTo-Json
Invoke-RestMethod -Method Post "$base/api/v1/auth/login" -WebSession $session -Headers $headers -ContentType "application/json" -Body $body
$plain = $null; $body = $null
Invoke-RestMethod "$base/api/v1/auth/me" -WebSession $session
$csrf = Invoke-RestMethod "$base/api/v1/auth/csrf" -WebSession $session
Invoke-RestMethod -Method Post "$base/api/v1/auth/logout" -WebSession $session -Headers @{ $csrf.headerName = $csrf.token }
```

Operações mutáveis sem o header CSRF retornam `403`. Login inválido, conta desconhecida, inativa ou bloqueada usam a mesma resposta genérica.

## Executar fora do Docker

Inicie um PostgreSQL compatível e defina `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e as variáveis de bootstrap. No PowerShell:

```powershell
$env:JAVA_HOME = "C:\caminho\para\jdk-21"
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Profiles disponíveis: `dev` (default local/Compose), `test` (datasource fornecido por Testcontainers) e `prod` (cookie Secure, mesma origem e variáveis de banco obrigatórias). Configure com `SPRING_PROFILES_ACTIVE`.

## Testes

```powershell
$env:JAVA_HOME = "C:\caminho\para\jdk-21"
Set-Location backend
.\mvnw.cmd clean verify
```

Os testes unitários não exigem Docker. Os testes `*IntegrationTest` usam PostgreSQL real por Testcontainers; quando o daemon não existe, são marcados como ignorados pelo suporte do Testcontainers e essa limitação deve ser tratada como verificação pendente, não como evidência de integração aprovada.

Com a stack saudável, exporte as credenciais do administrador somente no processo atual e rode:

```powershell
$env:GLAZR_BOOTSTRAP_ADMIN_EMAIL = Read-Host "E-mail do administrador"
$secure = Read-Host "Senha do administrador" -AsSecureString
$env:GLAZR_BOOTSTRAP_ADMIN_PASSWORD = [Net.NetworkCredential]::new("", $secure).Password
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
Remove-Item Env:GLAZR_BOOTSTRAP_ADMIN_PASSWORD
```

## Contrato principal

- autenticação: `GET /api/v1/auth/csrf`, `POST /login`, `GET /me`, `POST /logout`;
- usuários: `POST/GET /api/v1/users`, `GET/PATCH /users/{id}`, `PATCH /users/{id}/status`;
- perfis: `POST/GET /api/v1/access-profiles`, `GET/PUT/DELETE /access-profiles/{id}`;
- permissões: `GET /api/v1/permissions`.

Erros usam `application/problem+json` com `code` estável e sem stack trace ou credenciais. Consulte [a spec](docs/specs/backend-authentication-foundation.md) e [a arquitetura](docs/architecture.md) para detalhes.

## Limitações desta etapa

Não existem frontend, cadastro público, confirmação de e-mail, recuperação/redefinição/alteração de senha, OAuth/SSO/MFA, JWT, multiempresa, e-mail, exclusão física de usuário ou módulos comerciais. Frontend React/TypeScript e redefinição de senha permanecem próximos passos separados desta entrega.
