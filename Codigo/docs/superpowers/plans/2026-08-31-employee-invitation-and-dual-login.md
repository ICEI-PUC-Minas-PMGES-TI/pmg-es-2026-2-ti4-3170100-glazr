# Employee Invitation and Dual Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar criação administrativa de funcionários, convite por e-mail, senha local e Google OIDC convergindo para o mesmo usuário, além de alteração/reset de senha e telas completas.

**Architecture:** `users` permanece a identidade central e credenciais opcionais ficam em tabelas próprias. Convites/resets usam tokens hash-only e consumo transacional; e-mail sai após commit; Google é mapeado por `sub` para usuário local preexistente e termina na sessão Spring existente.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Security OAuth2 Client, Spring Mail, Spring Session JDBC, PostgreSQL 17/Flyway, React 19/TypeScript strict, Vitest/MSW, Docker Compose, Nginx e Mailpit.

**Spec:** `docs/specs/employee-invitation-and-dual-login.md`

## Global Constraints

- Nunca criar `users` a partir de Google; senha e Google devem resolver o mesmo UUID local.
- Não editar migrations V1–V3; Flyway é o único proprietário do schema.
- Tokens temporários usam 32 bytes aleatórios, Base64 URL-safe sem padding e SHA-256 persistido.
- Não persistir ou expor token OAuth, token temporário cru, hash, senha, segredo ou identificador de sessão.
- CSRF, CORS, `GLAZR_SESSION`, PostgreSQL e revalidação de permissions permanecem ativos.
- Google usa somente `openid email profile`, exige `email_verified=true` e identifica por `sub`.
- Toda interface e mensagem pública usa português e evita enumeração de contas.
- `.env` e o arquivo preexistente `PROMPT_FRONTEND_AUTENTICACAO.md` não entram nos commits.

---

### Task 1: Migrations de identidade e compatibilidade V3

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__separate_user_credentials.sql`
- Create: `backend/src/main/resources/db/migration/V5__create_invitation_and_external_identity_schema.sql`
- Create: `backend/src/test/java/com/glazr/integration/IdentityMigrationIntegrationTest.java`

**Interfaces:**
- Produces: `users.account_type`, `user_password_credentials`, `user_external_identities`, `employee_invitations` e `password_reset_requests` com constraints da spec.

- [ ] Escrever teste Testcontainers que aplica V1–V3, insere usuário/hash conhecido, executa V4–V5 e verifica `EMPLOYEE`, hash copiado e coluna antiga removida.
- [ ] Executar `backend/mvnw.cmd -Dit.test=IdentityMigrationIntegrationTest verify` e confirmar RED por migrations ausentes.
- [ ] Criar V4 transacional: adicionar/backfill/validar `account_type`, criar/copiar credenciais e remover `users.password_hash` somente após cópia.
- [ ] Criar V5 com FKs, checks, uniques e índices parciais para identidade, convite e reset utilizáveis.
- [ ] Executar o teste focal e depois `backend/mvnw.cmd clean verify`; confirmar upgrade e banco vazio verdes.
- [ ] Commit: `feat(identity): migrate credentials and temporary tokens`.

### Task 2: Entidades de usuário e credenciais locais

**Files:**
- Create: `backend/src/main/java/com/glazr/user/domain/AccountType.java`
- Create: `backend/src/main/java/com/glazr/credential/domain/AuthenticationMethod.java`
- Create: `backend/src/main/java/com/glazr/credential/infrastructure/PasswordCredentialEntity.java`
- Create: `backend/src/main/java/com/glazr/credential/infrastructure/PasswordCredentialRepository.java`
- Create: `backend/src/main/java/com/glazr/credential/infrastructure/ExternalIdentityEntity.java`
- Create: `backend/src/main/java/com/glazr/credential/infrastructure/ExternalIdentityRepository.java`
- Modify: `backend/src/main/java/com/glazr/user/infrastructure/UserEntity.java`
- Modify: `backend/src/main/java/com/glazr/user/api/UserResponse.java`
- Test: `backend/src/test/java/com/glazr/user/infrastructure/UserEntityTest.java`
- Create: `backend/src/test/java/com/glazr/credential/infrastructure/CredentialEntityTest.java`

**Interfaces:**
- Produces: `AccountType.EMPLOYEE`, `AuthenticationMethod.PASSWORD/GOOGLE`, `PasswordCredentialEntity.create/change` e `ExternalIdentityEntity.linkGoogle/touchLogin`.

- [ ] Escrever testes que criam funcionário sem senha, rejeitam conta sem perfil e validam normalização/constraints das credenciais.
- [ ] Executar os testes focais e confirmar RED por tipos ausentes.
- [ ] Remover `passwordHash` de `UserEntity`, adicionar `accountType`, manter fábrica de bootstrap e criar fábrica `createEmployee` sem credencial.
- [ ] Implementar entidades/repositories focados, sem expor hashes em DTOs ou `toString`.
- [ ] Evoluir `UserResponse` com tipo e métodos calculados por um mapper de aplicação, evitando repository dentro do record.
- [ ] Executar testes focais e suíte unitária; commit `feat(identity): model local and external credentials`.

### Task 3: Login local e bootstrap compatíveis

**Files:**
- Modify: `backend/src/main/java/com/glazr/authentication/application/AuthenticationService.java`
- Modify: `backend/src/main/java/com/glazr/authentication/application/AuthenticationTransaction.java`
- Modify: `backend/src/main/java/com/glazr/authentication/application/AuthenticatedUser.java`
- Modify: `backend/src/main/java/com/glazr/authentication/application/AuthenticatedUserFactory.java`
- Modify: `backend/src/main/java/com/glazr/user/application/AdministratorBootstrapService.java`
- Test: `backend/src/test/java/com/glazr/authentication/application/AuthenticationServiceTest.java`
- Test: `backend/src/test/java/com/glazr/user/application/AdministratorBootstrapServiceTest.java`
- Test: `backend/src/test/java/com/glazr/integration/BackendIntegrationTest.java`

**Interfaces:**
- Consumes: `PasswordCredentialRepository`.
- Produces: `AuthenticatedUser(..., accountType, authenticationMethods)` e bootstrap com credencial separada.

- [ ] Alterar testes para exigir login existente via credencial migrada, falha genérica sem credencial e DTO com `PASSWORD`.
- [ ] Executar testes focais e confirmar RED.
- [ ] Fazer leitura de hash por credencial, manter comparação sentinela e revalidar hash sob lock antes de registrar tentativa.
- [ ] Fazer bootstrap salvar usuário e `PasswordCredentialEntity` na mesma transação.
- [ ] Atualizar factory para consultar métodos sem introduzir lazy loading fora da transação.
- [ ] Rodar testes focais e integração; commit `refactor(auth): authenticate through credential records`.

### Task 4: Serviço de tokens e domínio de convites/resets

**Files:**
- Create: `backend/src/main/java/com/glazr/credential/domain/TemporaryToken.java`
- Create: `backend/src/main/java/com/glazr/credential/application/TemporaryTokenService.java`
- Create: `backend/src/main/java/com/glazr/invitation/domain/InvitationStatus.java`
- Create: `backend/src/main/java/com/glazr/invitation/domain/DeliveryStatus.java`
- Create: `backend/src/main/java/com/glazr/invitation/infrastructure/EmployeeInvitationEntity.java`
- Create: `backend/src/main/java/com/glazr/invitation/infrastructure/EmployeeInvitationRepository.java`
- Create: `backend/src/main/java/com/glazr/credential/infrastructure/PasswordResetEntity.java`
- Create: `backend/src/main/java/com/glazr/credential/infrastructure/PasswordResetRepository.java`
- Create: `backend/src/test/java/com/glazr/credential/application/TemporaryTokenServiceTest.java`
- Create: `backend/src/test/java/com/glazr/invitation/infrastructure/EmployeeInvitationEntityTest.java`

**Interfaces:**
- Produces: `TemporaryToken(rawValue, hash)`, `generate()`, `hash(String)`, `EmployeeInvitationEntity.create/consume/revoke/markSent/markFailed/statusAt` e equivalentes de reset.

- [ ] Escrever testes determinísticos com `SecureRandom` injetável/adapter, vetores SHA-256, expiração, revogação e consumo único.
- [ ] Confirmar RED antes de criar implementações.
- [ ] Implementar geração/hash sem logs e entidades com relógio fornecido pelo caso de uso.
- [ ] Adicionar queries `findByTokenHashForUpdate`, `findUsableByUserIdForUpdate` e revogação explícita.
- [ ] Executar testes e commit `feat(identity): add secure invitation and reset tokens`.

### Task 5: Porta de e-mail, SMTP e Mailpit

**Files:**
- Create: `backend/src/main/java/com/glazr/mail/application/EmployeeMailSender.java`
- Create: `backend/src/main/java/com/glazr/mail/application/EmployeeInvitationMessage.java`
- Create: `backend/src/main/java/com/glazr/mail/application/PasswordResetMessage.java`
- Create: `backend/src/main/java/com/glazr/mail/infrastructure/SmtpEmployeeMailSender.java`
- Create: `backend/src/main/java/com/glazr/mail/infrastructure/PublicBaseUrl.java`
- Create: `backend/src/main/resources/templates/mail/employee-invitation.txt`
- Create: `backend/src/main/resources/templates/mail/employee-invitation.html`
- Create: `backend/src/main/resources/templates/mail/password-reset.txt`
- Create: `backend/src/main/resources/templates/mail/password-reset.html`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `compose.yaml`
- Modify: `.env.example`
- Create: `backend/src/test/java/com/glazr/mail/infrastructure/PublicBaseUrlTest.java`

**Interfaces:**
- Produces: `EmployeeMailSender.sendInvitation(EmployeeInvitationMessage)` e `sendPasswordReset(PasswordResetMessage)`.

- [ ] Escrever testes para rejeitar base URL com query/fragmento/credenciais e montar links com fragmento.
- [ ] Confirmar RED; adicionar Spring Mail e Thymeleaf somente se necessário para templates seguros.
- [ ] Implementar adapter multipart texto/HTML sem registrar corpo/link.
- [ ] Adicionar Mailpit fixado, healthcheck, SMTP interno e porta HTTP configurável; não publicar SMTP.
- [ ] Adicionar placeholders inválidos e flags SMTP/TTL/Google à `.env.example`.
- [ ] Rodar testes, `docker compose --env-file .env config` e commit `feat(mail): add SMTP adapter and Mailpit`.

### Task 6: Criação administrativa e ciclo de convite

**Files:**
- Modify: `backend/src/main/java/com/glazr/user/api/CreateUserRequest.java`
- Modify: `backend/src/main/java/com/glazr/user/application/UserManagementService.java`
- Modify: `backend/src/main/java/com/glazr/user/api/UserResponse.java`
- Create: `backend/src/main/java/com/glazr/invitation/api/InvitationAdminController.java`
- Create: `backend/src/main/java/com/glazr/invitation/api/InvitationResponse.java`
- Create: `backend/src/main/java/com/glazr/invitation/application/EmployeeInvitationService.java`
- Create: `backend/src/main/java/com/glazr/invitation/application/InvitationDeliveryListener.java`
- Test: `backend/src/test/java/com/glazr/user/application/UserManagementServiceTest.java`
- Create: `backend/src/test/java/com/glazr/invitation/application/EmployeeInvitationServiceTest.java`
- Modify: `backend/src/test/java/com/glazr/integration/BackendIntegrationTest.java`

**Interfaces:**
- Produces: criação sem senha; `get`, `resend`, `revoke`; evento pós-commit com token cru somente em memória.

- [ ] Escrever testes para payload sem senha, duplicidade, convite PENDING, falha SMTP preservando usuário e reenvio revogando token anterior.
- [ ] Confirmar RED nos testes focais.
- [ ] Implementar transação user+convite e listener `AFTER_COMMIT`; traduzir falha para `FAILED` em nova transação.
- [ ] Adicionar endpoints e `@PreAuthorize` conforme spec; manter CSRF global.
- [ ] Rodar testes focais e integração; commit `feat(users): create employees through invitations`.

### Task 7: Inspeção e ativação por senha

**Files:**
- Create: `backend/src/main/java/com/glazr/invitation/api/InvitationController.java`
- Create: `backend/src/main/java/com/glazr/invitation/api/InspectInvitationRequest.java`
- Create: `backend/src/main/java/com/glazr/invitation/api/InvitationInspectionResponse.java`
- Create: `backend/src/main/java/com/glazr/invitation/api/AcceptInvitationWithPasswordRequest.java`
- Create: `backend/src/main/java/com/glazr/invitation/application/InvitationActivationService.java`
- Create: `backend/src/main/java/com/glazr/authentication/application/SessionAuthenticationService.java`
- Modify: `backend/src/main/java/com/glazr/authentication/api/AuthenticationController.java`
- Modify: `backend/src/main/java/com/glazr/configuration/SecurityConfiguration.java`
- Create: `backend/src/test/java/com/glazr/invitation/application/InvitationActivationServiceTest.java`
- Modify: `backend/src/test/java/com/glazr/integration/BackendIntegrationTest.java`

**Interfaces:**
- Produces: `inspect(rawToken)` e `acceptWithPassword(rawToken, password)`; `SessionAuthenticationService.establish(user, request, response)` reutilizado por todos os métodos.

- [ ] Escrever testes para dados mascarados, inválido/expirado/revogado/usado, consumo concorrente e sessão do mesmo UUID.
- [ ] Confirmar RED.
- [ ] Implementar lock, política de senha, credencial e consumo em uma transação; controller estabelece sessão após sucesso.
- [ ] Liberar apenas endpoints públicos necessários; CSRF continua exigido em POST.
- [ ] Executar testes e commit `feat(invitations): activate employees with local password`.

### Task 8: Alteração e redefinição de senha

**Files:**
- Create: `backend/src/main/java/com/glazr/credential/api/PasswordController.java`
- Create: `backend/src/main/java/com/glazr/credential/api/ChangePasswordRequest.java`
- Create: `backend/src/main/java/com/glazr/credential/api/PasswordResetRequest.java`
- Create: `backend/src/main/java/com/glazr/credential/api/CompletePasswordResetRequest.java`
- Create: `backend/src/main/java/com/glazr/credential/application/PasswordManagementService.java`
- Modify: `backend/src/main/java/com/glazr/authentication/infrastructure/UserSessionService.java`
- Create: `backend/src/test/java/com/glazr/credential/application/PasswordManagementServiceTest.java`
- Modify: `backend/src/test/java/com/glazr/integration/BackendIntegrationTest.java`

**Interfaces:**
- Produces: `change(userId,current,new,currentSessionId)`, `requestReset(email)` e `completeReset(token,new)`; exclusão de todas ou todas exceto a sessão corrente.

- [ ] Escrever testes de senha atual, ausência de credencial, não reutilização, resposta 204 genérica, rate limit, criação de primeira senha e invalidação de sessões.
- [ ] Confirmar RED.
- [ ] Implementar alteração transacional e reset pós-commit com e-mail; comparar sentinela para entradas não elegíveis quando apropriado.
- [ ] Expor os três endpoints com authorization/CSRF corretos e códigos estáveis.
- [ ] Rodar testes e commit `feat(auth): add password change and reset flows`.

### Task 9: Google OIDC e garantia de identidade única

**Files:**
- Create: `backend/src/main/java/com/glazr/federatedidentity/domain/GoogleIdentityClaims.java`
- Create: `backend/src/main/java/com/glazr/federatedidentity/application/GoogleIdentityService.java`
- Create: `backend/src/main/java/com/glazr/federatedidentity/infrastructure/GoogleOidcUserService.java`
- Create: `backend/src/main/java/com/glazr/federatedidentity/infrastructure/GoogleAuthenticationSuccessHandler.java`
- Create: `backend/src/main/java/com/glazr/federatedidentity/infrastructure/GoogleAuthenticationFailureHandler.java`
- Create: `backend/src/main/java/com/glazr/federatedidentity/api/GoogleAuthController.java`
- Modify: `backend/src/main/java/com/glazr/configuration/SecurityConfiguration.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/pom.xml`
- Create: `backend/src/test/java/com/glazr/federatedidentity/application/GoogleIdentityServiceTest.java`
- Create: `backend/src/test/java/com/glazr/federatedidentity/infrastructure/GoogleOidcUserServiceTest.java`
- Modify: `backend/src/test/java/com/glazr/integration/BackendIntegrationTest.java`

**Interfaces:**
- Produces: `resolveOrLink(GoogleIdentityClaims, Optional<UUID> invitationId)` sem criação de usuário e callbacks que criam sessão Glazr.

- [ ] Escrever testes para `email_verified=false`, mismatch, lookup por sub, primeiro vínculo por e-mail, sub conflitante, callbacks concorrentes, usuário inativo e mesmo UUID/permissions.
- [ ] Confirmar RED antes de adicionar OAuth2 Client.
- [ ] Implementar flags validadas, início de convite guardando apenas invitation ID e mapper de claims sem tokens.
- [ ] Configurar scopes exatos, handlers internos e redirect seguro; Google desabilitado não registra botão/fluxo.
- [ ] Rodar testes sem rede externa e commit `feat(auth): link Google OIDC to local employees`.

### Task 10: Contratos e navegação frontend

**Files:**
- Modify: `frontend/src/features/auth/model/auth-types.ts`
- Modify: `frontend/src/features/auth/model/AuthProvider.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/pages/HomePage.tsx`
- Create: `frontend/src/shared/components/AppShell.tsx`
- Create: `frontend/src/app/PermissionRoute.tsx`
- Modify: `frontend/src/app/authentication-flow.test.tsx`

**Interfaces:**
- Produces: modelo com `accountType`/`authenticationMethods`, shell autenticado e guard por permission.

- [ ] Atualizar fixtures e escrever teste de navegação visível somente com `USER_READ`; confirmar RED.
- [ ] Implementar contratos compatíveis, shell e guard com fallback seguro.
- [ ] Executar teste focal, lint e typecheck; commit `feat(frontend): extend authenticated account model`.

### Task 11: Administração de funcionários no frontend

**Files:**
- Create: `frontend/src/features/users/api/users-api.ts`
- Create: `frontend/src/features/users/model/user-types.ts`
- Create: `frontend/src/features/users/components/UserList.tsx`
- Create: `frontend/src/features/users/components/EmployeeForm.tsx`
- Create: `frontend/src/pages/AdminUsersPage.tsx`
- Create: `frontend/src/pages/NewEmployeePage.tsx`
- Create: `frontend/src/features/users/users-flow.test.tsx`
- Modify: `frontend/src/app/router.tsx`
- Create: `frontend/src/features/users/components/Users.css`

**Interfaces:**
- Produces: `/admin/users`, `/admin/users/new`, criação sem password e ações de convite baseadas em permission.

- [ ] Escrever testes MSW para acesso permitido/negado, payload sem senha, falha SMTP preservada, reenvio e revogação.
- [ ] Confirmar RED.
- [ ] Implementar API centralizada, paginação/lista, formulário acessível e feedback sem bibliotecas visuais novas.
- [ ] Executar testes, lint, typecheck e commit `feat(frontend): add employee administration`.

### Task 12: Ativação, Google, reset e segurança da conta no frontend

**Files:**
- Create: `frontend/src/features/invitations/api/invitations-api.ts`
- Create: `frontend/src/features/invitations/hooks/use-fragment-token.ts`
- Create: `frontend/src/pages/ActivatePage.tsx`
- Create: `frontend/src/pages/ForgotPasswordPage.tsx`
- Create: `frontend/src/pages/ResetPasswordPage.tsx`
- Create: `frontend/src/pages/AccountSecurityPage.tsx`
- Create: `frontend/src/features/account-security/api/account-security-api.ts`
- Modify: `frontend/src/features/auth/components/LoginForm.tsx`
- Modify: `frontend/src/app/router.tsx`
- Create: `frontend/src/features/invitations/invitation-flow.test.tsx`
- Create: `frontend/src/features/account-security/account-security-flow.test.tsx`
- Create: `frontend/src/features/invitations/components/Invitation.css`

**Interfaces:**
- Produces: remoção imediata do fragmento, ativação por senha/Google, forgot/reset anti-enumeração, mudança/criação de senha e login Google condicional.

- [ ] Escrever testes que verificam `history.replaceState` antes da chamada, estados de convite, ativação, botão Google condicional e mensagens genéricas.
- [ ] Confirmar RED.
- [ ] Implementar hooks/APIs/páginas mantendo token só em memória e impedindo submissões repetidas.
- [ ] Integrar resposta autenticada ao `AuthProvider` sem armazenamento persistente.
- [ ] Rodar todos os testes frontend, lint, typecheck, build e checklist React; commit `feat(frontend): add invitation and account security flows`.

### Task 13: SMTP real local, smoke, documentação e entrega

**Files:**
- Create: `backend/src/test/java/com/glazr/integration/MailDeliveryIntegrationTest.java`
- Modify: `scripts/smoke-test.ps1`
- Modify: `README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/specs/employee-invitation-and-dual-login.md`

**Interfaces:**
- Produces: operação local documentada e smoke de convite/senha/reset; checklist manual Google separado.

- [ ] Testar mensagem realmente recebida, destinatário, assunto, texto/HTML e links diferentes no reenvio sem registrar token.
- [ ] Atualizar README com Mailpit, SMTP externo, flags, setup Google dev/prod, redirect URIs, Workspace e ausência de Firebase.
- [ ] Atualizar smoke para admin → funcionário → Mailpit → ativação → login → senha/reset → sessões, sem imprimir segredos.
- [ ] Executar `backend/mvnw.cmd clean verify` e confirmar zero falhas/skips indevidos.
- [ ] Executar `npm ci`, `npm run lint`, `npm run typecheck`, `npm run test:run` e `npm run build`.
- [ ] Executar `docker compose --env-file .env config`, build, `up -d --wait`, `docker compose ps` e smoke pelo Nginx.
- [ ] Executar `git diff --check`, revisar status/diff/logs e procurar segredos/tokens/links completos.
- [ ] Commit: `docs: document employee identity operations`.
- [ ] Sincronizar com `origin/main` sem force-push e repetir verificações afetadas antes do envio, caso o remoto tenha avançado.

## Auto-revisão do plano

- Cobertura: migrations, identidade, credencial local, convite, SMTP, Google, senha/reset, sessões, frontend, Mailpit, documentação e smoke possuem tarefas próprias.
- Placeholders: não há TBD/TODO nem integração real Google simulada como concluída; somente a configuração externa permanece manual por depender de credenciais.
- Consistência: `AccountType`, `AuthenticationMethod`, `TemporaryTokenService`, `EmployeeInvitationService`, `SessionAuthenticationService` e `GoogleIdentityService` mantêm nomes e responsabilidades únicos.
- Segurança: nenhuma tarefa envia token de convite em query ao Google, persiste token OAuth ou cria usuário no callback.
