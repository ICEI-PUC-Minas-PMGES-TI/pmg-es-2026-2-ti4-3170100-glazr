# Convite de funcionário e autenticação dual do Glazr

## 1. Propósito e limites

Esta especificação evolui a identidade dos funcionários do Glazr sem alterar sua premissa central: `users` é a fonte de verdade; senha local e Google são credenciais vinculadas ao mesmo UUID. Um login externo nunca cria automaticamente um funcionário e nunca redefine perfis ou permissões.

A entrega inclui administração de funcionários, convite temporário por e-mail, ativação por senha ou Google OpenID Connect, vínculo posterior de credenciais, alteração e redefinição de senha, SMTP/Mailpit e as telas correspondentes. Permanecem fora do escopo clientes, autocadastro, marketing, orçamento, MFA, JWT, multiempresa, Firebase e APIs Google além de `openid email profile`.

## 2. Decisões arquiteturais

- O backend continua sendo um monólito modular Java 21/Spring Boot 3.5.16 com PostgreSQL, Flyway, Spring Security e Spring Session JDBC.
- A SPA permanece React/TypeScript strict, usa somente a API de mesma origem e mantém autenticação em memória.
- `users.account_type` classifica todas as contas atuais como `EMPLOYEE`; `CUSTOMER` é somente um valor reservado para evolução futura.
- Credenciais locais ficam em `user_password_credentials`, em relação opcional um-para-um com `users`.
- Identidades externas ficam em `user_external_identities`; nesta etapa somente `GOOGLE` é aceito.
- Convites e redefinições possuem tabelas e ciclos de vida separados.
- Google comprova identidade; o Glazr mantém sessão, autorização e dados de negócio.
- Nenhum access token ou refresh token Google é persistido ou retornado ao frontend.
- Envio de e-mail ocorre depois do commit por uma porta de aplicação e adapter SMTP.

## 3. Modelo persistente e migrations

As migrations `V1`, `V2` e `V3` são imutáveis. Novas migrations devem funcionar tanto em banco vazio quanto em upgrade de V3.

### 3.1 Usuário

`users` recebe `account_type VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE'`, com constraint que inicialmente admite `EMPLOYEE` e `CUSTOMER`. Todas as linhas existentes são migradas para `EMPLOYEE`.

### 3.2 Credencial local

```text
user_password_credentials
  user_id UUID PK/FK users ON DELETE CASCADE
  password_hash VARCHAR(100) NOT NULL
  password_changed_at TIMESTAMPTZ NOT NULL
  created_at TIMESTAMPTZ NOT NULL
  updated_at TIMESTAMPTZ NOT NULL
```

Hashes existentes são copiados sem re-encode. Somente depois da cópia, `users.password_hash` é removida. Ausência de linha significa ausência de senha; não existem hashes nulos, vazios ou placeholders.

### 3.3 Identidade externa

```text
user_external_identities
  id UUID PK
  user_id UUID NOT NULL FK users ON DELETE CASCADE
  provider VARCHAR(30) NOT NULL
  subject VARCHAR(255) NOT NULL
  email_at_link VARCHAR(320) NOT NULL
  linked_at TIMESTAMPTZ NOT NULL
  last_login_at TIMESTAMPTZ
```

O banco impõe unicidade em `(provider, subject)` e `(user_id, provider)`. A aplicação limita `provider` a `GOOGLE`, normaliza `email_at_link` e usa `sub` como identificador permanente.

### 3.4 Convite

```text
employee_invitations
  id UUID PK
  user_id UUID NOT NULL FK users
  token_hash CHAR(64) NOT NULL UNIQUE
  expires_at TIMESTAMPTZ NOT NULL
  consumed_at TIMESTAMPTZ
  revoked_at TIMESTAMPTZ
  delivery_status VARCHAR(20) NOT NULL
  delivery_error_code VARCHAR(80)
  sent_at TIMESTAMPTZ
  created_by_user_id UUID NOT NULL FK users
  created_at TIMESTAMPTZ NOT NULL
```

`delivery_status` aceita `PENDING`, `SENT` e `FAILED`. O estado funcional é derivado de consumo, revogação e expiração. Um índice único parcial impede mais de um convite não consumido e não revogado por usuário. Reenvio revoga o anterior antes de criar um novo.

### 3.5 Redefinição de senha

```text
password_reset_requests
  id UUID PK
  user_id UUID NOT NULL FK users
  token_hash CHAR(64) NOT NULL UNIQUE
  expires_at TIMESTAMPTZ NOT NULL
  consumed_at TIMESTAMPTZ
  revoked_at TIMESTAMPTZ
  created_at TIMESTAMPTZ NOT NULL
```

Um índice único parcial impede mais de um reset utilizável por usuário. Convites e resets nunca compartilham tokens.

## 4. Tokens temporários

`TemporaryTokenService` gera 32 bytes com `SecureRandom`, codifica em Base64 URL-safe sem padding e persiste somente SHA-256 hexadecimal em minúsculas. O token cru existe apenas durante a construção do e-mail e nunca aparece em logs, resposta administrativa ou banco.

- convite: 48 horas por padrão, configurável em `GLAZR_INVITATION_TTL`;
- reset: 30 minutos por padrão, configurável em `GLAZR_PASSWORD_RESET_TTL`;
- consumo: lock pessimista e atualização na mesma transação da credencial/vínculo;
- uso único: consumido, revogado ou expirado falha com código estável;
- URLs: `${GLAZR_PUBLIC_BASE_URL}/activate#token=...` e `/reset-password#token=...`.

A SPA lê o fragmento uma vez, chama `history.replaceState` imediatamente e mantém o token somente em memória até a operação terminar.

## 5. Criação e convite administrativo

`POST /api/v1/users` passa a receber `email`, `displayName` e `accessProfileIds`, sem senha. Exige `USER_CREATE` e `ACCESS_PROFILE_ASSIGN`.

Na transação principal:

1. normaliza e valida o e-mail;
2. recusa duplicidade case-insensitive;
3. valida perfis ativos;
4. cria `EMPLOYEE` ativo, ainda sem credencial;
5. cria convite `PENDING` com hash e validade;
6. registra evento pós-commit contendo apenas dados necessários ao envio.

O adapter envia texto e HTML. Sucesso marca `SENT`; falha marca `FAILED` e um código seguro, sem apagar usuário ou convite.

Endpoints adicionais:

```text
GET  /api/v1/users/{id}/invitation
POST /api/v1/users/{id}/invitation/resend
POST /api/v1/users/{id}/invitation/revoke
```

Leitura exige `USER_READ`; reenvio e revogação exigem `USER_UPDATE`. Reenvio sempre gera novo token e invalida o anterior. Usuário que já possui senha ou Google não recebe novo convite de ativação.

## 6. Aceitação por senha

Endpoints públicos, ainda sujeitos a CSRF nas mutações:

```text
POST /api/v1/invitations/inspect
POST /api/v1/invitations/accept-with-password
```

`inspect` retorna somente nome parcial, e-mail mascarado, validade e métodos disponíveis. `accept-with-password` valida token e usuário `EMPLOYEE` ativo, aplica a política de 12–128 caracteres, cria a credencial local e consome o convite atomicamente. A experiência escolhida é autenticação imediata: o backend troca o ID da sessão, salva o mesmo principal Glazr usado no login local e retorna `AuthenticatedUser`.

## 7. Google OpenID Connect

Spring Security OAuth2 Client integra diretamente com Google usando somente `openid`, `email` e `profile`. A funcionalidade é governada por:

```text
GLAZR_GOOGLE_AUTH_ENABLED
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GLAZR_PUBLIC_BASE_URL
```

Desabilitado, o backend inicia normalmente e a SPA oculta o botão. Habilitado sem configuração completa, o startup falha mencionando apenas os nomes ausentes.

### 7.1 Convite iniciado com Google

`POST /api/v1/invitations/google/start` recebe o token no corpo, valida o convite e guarda na sessão somente seu UUID interno. O navegador segue para `/oauth2/authorization/google`; Spring controla `state` e `nonce`. No callback, o backend exige issuer/audience/assinatura/expiração válidos, `email_verified=true` e igualdade case-insensitive entre e-mail Google e e-mail local. Em sucesso, cria a identidade para o mesmo usuário, consome o convite e cria a sessão Glazr. Divergência não consome convite.

### 7.2 Login e vínculo posterior

O callback procura primeiro `(GOOGLE, sub)`. Se encontrar, valida usuário ativo `EMPLOYEE` e igualdade do e-mail atual com o local. Se não encontrar, procura funcionário preexistente pelo e-mail verificado e cria somente o vínculo externo. Nunca cria linha em `users`.

Constraints e transação traduzem concorrência para `GOOGLE_IDENTITY_ALREADY_LINKED`. Um `sub` não pode pertencer a dois usuários e um usuário não pode possuir dois `sub` Google nesta etapa.

Após sucesso, o backend descarta tokens do provedor e cria a mesma sessão Spring usada pelo login local. O redirect final é interno e não contém token.

## 8. Senha e sessões

`POST /api/v1/auth/password/change` exige sessão, CSRF, senha local existente, senha atual correta e nova senha diferente. Atualiza a credencial e invalida todas as outras sessões, mantendo apenas a sessão corrente.

```text
POST /api/v1/auth/password-reset/request
POST /api/v1/auth/password-reset/complete
```

`request` sempre retorna `204`, inclusive para usuário ausente, inativo ou não elegível. Para funcionário ativo, revoga solicitação anterior, limita novas solicitações em uma janela configurável/testável e envia depois do commit. `complete` valida token, cria ou substitui a credencial, consome/revoga resets e invalida todas as sessões.

Usuário inicialmente Google cria sua primeira senha por reset de e-mail; nunca informa uma senha atual inexistente.

## 9. Contratos HTTP

`AuthenticatedUser` contém:

```text
id, email, displayName, active, accountType,
profiles, permissions, authenticationMethods
```

`UserResponse` acrescenta `accountType`, `authenticationMethods`, `invitationStatus`, `invitationExpiresAt` e `invitationDeliveryStatus`. Nenhum contrato expõe hash, token, `sub`, segredo OAuth, token OAuth ou identificador de sessão.

Códigos estáveis incluem:

```text
INVITATION_INVALID
INVITATION_EXPIRED
INVITATION_REVOKED
INVITATION_ALREADY_USED
INVITATION_DELIVERY_FAILED
GOOGLE_EMAIL_MISMATCH
GOOGLE_IDENTITY_ALREADY_LINKED
GOOGLE_LOGIN_NOT_REGISTERED
GOOGLE_AUTHENTICATION_FAILED
PASSWORD_CREDENTIAL_NOT_CONFIGURED
PASSWORD_RESET_INVALID
PASSWORD_RESET_EXPIRED
```

Erros públicos de login e solicitação de reset permanecem anti-enumeração.

## 10. E-mail e configuração

Uma porta `EmployeeMailSender` expõe `sendInvitation` e `sendPasswordReset`. O adapter `JavaMailSender` usa:

```text
SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD,
SMTP_FROM_ADDRESS, SMTP_FROM_NAME, SMTP_TLS_ENABLED,
GLAZR_PUBLIC_BASE_URL
```

Templates possuem partes texto e HTML simples, sem scripts, senha, perfis ou permissões. `GLAZR_PUBLIC_BASE_URL` deve ser uma origem HTTP(S) absoluta sem query, fragmento ou credenciais.

Compose de desenvolvimento inclui Mailpit com versão fixada, SMTP somente na rede interna, interface web em `${MAILPIT_HTTP_PORT:-8025}` e healthcheck. Produção usa SMTP externo e não inclui Mailpit.

## 11. Frontend

Rotas protegidas:

- `/admin/users`: lista nome, e-mail, status, perfis, métodos e convite; ações respeitam permissions;
- `/admin/users/new`: cria funcionário sem senha e informa situação do envio;
- `/account/security`: mostra métodos e permite alterar/criar senha ou iniciar vínculo Google.

Rotas públicas:

- `/activate`: remove token da URL, inspeciona e oferece senha ou Google;
- `/forgot-password`: resposta sempre genérica;
- `/reset-password`: remove token da URL e define senha;
- `/login`: mantém senha local e adiciona Google apenas quando habilitado.

Páginas usam as camadas `features/*`, cliente HTTP centralizado, tokens visuais existentes, formulários acessíveis, guards e mensagens em português. Permissões escondem ações, mas o backend continua sendo a autoridade.

## 12. Concorrência e segurança

- índices únicos case-insensitive e constraints são a barreira definitiva;
- convites e resets são consumidos sob lock, uma única vez;
- reenvio revoga o anterior na mesma transação;
- callback Google concorrente não duplica usuário nem identidade;
- envio SMTP nunca mantém transação de banco aberta;
- desativação invalida sessões e bloqueia senha e Google;
- alteração/reset de senha aplica a política de invalidação definida;
- CSRF permanece obrigatório em toda mutação de browser;
- CORS permanece restrito e produção usa mesma origem pelo Nginx;
- segredos, tokens crus e links completos não são registrados.

## 13. Estratégia de testes

- Unitários: tokens, hash, estados temporais, vínculo Google, claims, políticas de senha, URL pública e falha SMTP.
- Integração PostgreSQL: migrations vazias e upgrade V3, criação/convite, consumo concorrente, identidade única, sessões, permissions e CSRF.
- Integração SMTP: mensagem texto/HTML recebida no servidor local, destinatário/assunto/URL e reenvio.
- Frontend: autorização administrativa, criação sem senha, remoção de fragmento, ativação, Google, reset e acessibilidade.
- Smoke pelo Nginx: admin, convite Mailpit, ativação por senha, login, alteração/reset e revogação. Google real permanece verificação manual; a lógica de claims/vínculo é automatizada sem rede.

## 14. Critérios de aceite

A entrega está pronta quando um administrador cria um funcionário sem senha, o convite chega ao Mailpit, senha ou Google ativam o mesmo UUID local, reenvio invalida o link anterior, reset é anti-enumeração e invalida sessões, e nenhuma operação Google cria usuário. Backend, frontend, migrations, Compose e smoke tests devem passar sem segredos ou skips indevidos.

