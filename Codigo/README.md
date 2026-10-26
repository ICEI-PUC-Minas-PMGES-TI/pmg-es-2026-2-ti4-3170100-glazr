# Glazr

O Glazr é o sistema interno de uma única vidraçaria. A fundação atual inclui uma SPA responsiva, administração de funcionários, convites por e-mail, senha local e Google OIDC convergindo para o mesmo usuário, sessões persistidas, perfis configuráveis, PostgreSQL, Nginx e testes. Não há cadastro público.

## Tecnologias

- Java 21 e Spring Boot 3.5.16;
- Maven 3.9.11 por Maven Wrapper 3.3.2;
- PostgreSQL 17.11 e Flyway;
- Spring Security, OAuth2 Client/OIDC, Spring Session JDBC e SHA-256/Base64 + BCrypt força 12;
- Spring Mail e Mailpit 1.30.6 para e-mail local;
- React 19, TypeScript strict, Vite 8 e React Router;
- Vitest, Testing Library e MSW para uma suíte frontend enxuta;
- Testcontainers 1.21.4, gerenciado pelo Spring Boot;
- Node 24.15 no build do frontend, Docker Compose e Nginx stable 1.30.4.

O build não precisa de Maven, PostgreSQL ou Nginx instalados no host. Para executar fora de contêiner, é necessário um JDK 21 e acesso a PostgreSQL.

## Estrutura

```text
backend/                 aplicação Spring Boot, wrapper, migrations e testes
  src/main/java/com/glazr/
    authentication/      login local, principal e revalidação de sessão
    credential/          credenciais locais, identidades externas e tokens
    invitation/          convite, ativação e entrega pós-commit
    password/            alteração e redefinição de senha
    google/              vínculo e callback OpenID Connect
    mail/                porta de aplicação e adapter SMTP
    user/                usuários e proteção do último administrador
    accessprofile/       perfis configuráveis e perfil protegido
    permission/          catálogo somente leitura
    shared/              erros HTTP seguros
    configuration/       segurança e bootstrap
frontend/                SPA React/TypeScript
  src/
    app/                 composição, rotas e guards
    pages/               telas associadas às rotas
    features/            autenticação, convite, senha e administração
    shared/              cliente HTTP, componentes e estilos
docs/                    spec, arquitetura e plano de implementação
infra/nginx/             build da SPA, arquivos estáticos e proxy reverso
scripts/smoke-test.ps1   verificação ponta a ponta
compose.yaml             PostgreSQL, Mailpit, backend e Nginx para desenvolvimento
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

A aplicação fica disponível em `http://localhost:8080`, a API em `http://localhost:8080/api/v1` e o Mailpit local em `http://localhost:8025`. O Nginx serve a SPA e encaminha `/api/`, `/oauth2/` e `/login/oauth2/` ao backend pela mesma origem. A rota `http://localhost:8080/health` verifica o Nginx; backend e PostgreSQL não publicam portas no host.

Logs e parada sem apagar dados:

```powershell
docker compose logs -f backend
docker compose logs --no-color postgres backend nginx
docker compose down
```

> **Atenção — apaga dados:** `docker compose down -v` remove também o volume `glazr_postgres_data`. Execute esse comando somente quando quiser descartar definitivamente o banco local.

## Frontend e autenticação

A SPA possui `/login`, `/activate`, `/forgot-password`, `/reset-password`, `/account/security`, `/admin/users` e `/admin/users/new`. Ao abrir a aplicação, ela consulta `/api/v1/auth/me` para restaurar a sessão sem armazenar autenticação no navegador. Todas as mutações obtêm um CSRF novo e usam cookies de mesma origem com `credentials: include`.

O cookie `GLAZR_SESSION` é HttpOnly e nunca é lido pelo frontend. Senhas, tokens CSRF, convites e resets não são persistidos em `localStorage` ou `sessionStorage`. Tokens temporários chegam no fragmento, são copiados somente para memória e removidos imediatamente da barra de endereço.

Para desenvolver a interface com hot reload, mantenha a stack Docker ativa para atender a API e execute:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. O Vite encaminha `/api` para `http://localhost:8080`; a origem deve permanecer na allowlist local de `GLAZR_ALLOWED_ORIGINS`.

## Bootstrap do primeiro administrador

Quando `users` está vazia, o backend exige simultaneamente `GLAZR_BOOTSTRAP_ADMIN_EMAIL`, `GLAZR_BOOTSTRAP_ADMIN_PASSWORD` e `GLAZR_BOOTSTRAP_ADMIN_NAME`. Ele cria uma conta ativa associada ao perfil protegido `ADMINISTRADOR`, que já possui todas as permissões.

O processo é idempotente: quando existe algum usuário, as variáveis são ignoradas. Após a primeira inicialização, remova `GLAZR_BOOTSTRAP_ADMIN_PASSWORD` e, preferencialmente, todas as variáveis `GLAZR_BOOTSTRAP_ADMIN_*` do ambiente/`.env`, e recrie o contêiner do backend. A senha nunca é registrada em log.

```powershell
docker compose --env-file .env up -d --force-recreate backend
```

## Convites, SMTP e Mailpit

O administrador cria o funcionário sem senha em `/admin/users/new`. A operação persiste usuário e convite na mesma transação; o envio ocorre depois do commit. Falha SMTP não apaga o usuário e aparece como `FAILED`, permitindo reenvio com um token novo. Em desenvolvimento, abra o Mailpit em `http://localhost:8025`, leia a mensagem e use o link de ativação.

`GLAZR_PUBLIC_BASE_URL` deve conter somente a origem pública, sem caminho, query ou fragmento. Em produção configure SMTP externo pelas variáveis `SMTP_*`; o Compose deste repositório é a composição local de desenvolvimento e inclui Mailpit deliberadamente.

Para enviar pelo Gmail com uma senha de app, configure no `.env` local `SMTP_HOST=smtp.gmail.com`,
`SMTP_PORT=587`, `SMTP_USERNAME` com o endereço completo, `SMTP_PASSWORD` com a senha de app,
`SMTP_AUTH_ENABLED=true` e `SMTP_TLS_ENABLED=true`. `SMTP_FROM_ADDRESS` é opcional e, quando omitido,
usa o próprio `SMTP_USERNAME`. Nunca versione essas credenciais.

## Google OpenID Connect

Google fica desligado com `GLAZR_GOOGLE_AUTH_ENABLED=false`; nesse estado a aplicação inicia sem credenciais e oculta os botões. Para habilitar:

1. crie um projeto na Google Auth Platform e configure a tela de consentimento;
2. crie um cliente OAuth do tipo aplicação web;
3. registre exatamente `http://localhost:8080/login/oauth2/code/google` como redirect URI local;
4. defina `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` e `GLAZR_GOOGLE_AUTH_ENABLED=true` fora do Git;
5. recrie o backend.

São solicitados somente `openid`, `email` e `profile`. O Google deve retornar `email_verified=true`, e o e-mail precisa ser idêntico ao funcionário previamente cadastrado. O backend armazena apenas `provider` e `sub`; access token e refresh token não são persistidos nem enviados à SPA. Em produção use a origem HTTPS correspondente no redirect URI. Organizações Google Workspace podem exigir aprovação administrativa para aplicativos externos.

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

### Limitação de tentativas de login

O Nginx limita `POST /api/v1/auth/login` por IP de conexão, antes de encaminhar
ao backend: cinco tentativas iniciais em sequência e reposição de uma tentativa
a cada 12 segundos (`5r/m`). O excesso recebe `429`, código `LOGIN_RATE_LIMITED`
e header `Retry-After: 12`. A tela explica a espera e permite tentar novamente.
Tentativas bloqueadas não chegam ao BCrypt. CSRF, sessão e bloqueio de conta
por senha incorreta continuam ativos.

Essa proteção depende do ingresso Nginx usado no Compose. Ao executar o backend
diretamente, use também esse proxy; não exponha a porta do backend na internet.
O IP vem da conexão, não de `X-Forwarded-For` enviado pelo cliente. Usuários que
compartilham IP compartilham a cota. Se adicionar outro proxy à frente ou várias
réplicas do Nginx, revise a identificação de IP confiável e a distribuição da cota.

O teste do proxy inicia containers isolados, verifica bloqueio antes do upstream,
tentativas de contorno por headers/query string, recuperação da cota e acesso às
outras rotas. Execute da raiz do repositório, com Docker e Python 3:

```powershell
python -m unittest discover -s Codigo/infra/nginx/tests -v
```

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

Frontend, a partir de `frontend/`:

```powershell
npm ci
npm run lint
npm run typecheck
npm run test:run
npm run build
```

A suíte frontend é propositalmente enxuta e cobre contrato HTTP, login, falha segura, proteção de rota, sessão, logout e remoção imediata do token de convite. Não há meta artificial de cobertura.

Backend:

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

- autenticação: `GET /api/v1/auth/csrf`, `GET /auth/config`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`;
- convite: `POST /api/v1/invitations/inspect`, `/accept-with-password` e `/google/start`;
- senha: `POST /api/v1/auth/password/change`, `/password-reset/request` e `/password-reset/complete`;
- Google: `POST /api/v1/auth/google/start`, `/auth/google/link/start` e endpoints internos OAuth2;
- usuários: `POST/GET /api/v1/users`, `GET/PATCH /users/{id}`, `PATCH /users/{id}/status` e convite em `/users/{id}/invitation/*`;
- perfis: `POST/GET /api/v1/access-profiles`, `GET/PUT/DELETE /access-profiles/{id}`;
- permissões: `GET /api/v1/permissions`.

Erros usam `application/problem+json` com `code` estável e sem stack trace ou credenciais. Consulte [a spec](docs/specs/backend-authentication-foundation.md) e [a arquitetura](docs/architecture.md) para detalhes.

## Limitações desta etapa

Não existem cadastro público, MFA, JWT, multiempresa, exclusão física de usuário, identidade de clientes ou módulos comerciais. A validação automatizada do Google termina na fronteira de claims/vínculo e não usa rede externa; o teste final com credenciais reais deve ser feito manualmente no projeto Google configurado para o ambiente.
