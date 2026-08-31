# Backend Authentication Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar a fundação executável do backend Glazr com autenticação por sessão PostgreSQL, autorização configurável, APIs administrativas e infraestrutura Docker/Nginx.

**Architecture:** Monólito modular Spring Boot organizado por funcionalidade e camada, com casos de uso transacionais, JPA privado à infraestrutura e DTOs na API. Flyway controla todo o schema; Spring Security, Spring Session JDBC e uma revalidação de principal por requisição mantêm sessão, estado da conta e permissões consistentes.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven Wrapper 3.3.2, PostgreSQL 17.11, Flyway, Spring Security/Session JDBC, Testcontainers, Docker Compose e Nginx 1.30.4.

**Spec:** `docs/specs/backend-authentication-foundation.md`

## Global Constraints

- Usar Java 21, Spring Boot 3.5.16 e Maven Wrapper; não depender de Maven global.
- Manter PostgreSQL como banco de produção e integração; nenhum banco em memória.
- Hibernate usa `ddl-auto=validate`; Flyway é o único proprietário do DDL.
- Autenticação usa sessão Spring Session JDBC; não usar JWT, Redis ou serviços externos.
- Não usar Lombok ou MapStruct.
- Não criar frontend, cadastro público, mudança/recuperação de senha ou multi-tenancy.
- Escrever testes antes do código de comportamento e observar a falha esperada.
- Nunca registrar ou versionar senha, hash, cookie de sessão ou token CSRF.

---

### Task 1: Build reproduzível e aplicação mínima

**Files:**
- Create: `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`, `backend/.mvn/wrapper/maven-wrapper.jar`
- Create: `backend/src/main/java/com/glazr/GlazrApplication.java`
- Create: `backend/src/test/java/com/glazr/GlazrApplicationTests.java`

**Interfaces:**
- Produces: aplicação `com.glazr.GlazrApplication` e lifecycle Maven `verify` com Java 21.

- [ ] **Step 1: Adicionar o teste de contexto mínimo**

```java
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class GlazrApplicationTests {
    @Test void contextLoads() {}
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=GlazrApplicationTests test`
Expected: FAIL porque `GlazrApplication` e o build ainda não existem.

- [ ] **Step 3: Criar POM, wrapper e classe principal mínimos**

```java
@SpringBootApplication
public class GlazrApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlazrApplication.class, args);
    }
}
```

O POM importa os starters Web, Security, Data JPA, Validation, Session JDBC, Actuator, Flyway PostgreSQL, driver PostgreSQL e dependências de teste/Testcontainers.

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=GlazrApplicationTests test`
Expected: PASS, Java release 21.

### Task 2: Domínio básico e política de credenciais

**Files:**
- Create: `backend/src/main/java/com/glazr/user/domain/EmailAddress.java`
- Create: `backend/src/main/java/com/glazr/user/domain/PasswordPolicy.java`
- Create: `backend/src/main/java/com/glazr/permission/domain/PermissionKey.java`
- Test: `backend/src/test/java/com/glazr/user/domain/EmailAddressTest.java`
- Test: `backend/src/test/java/com/glazr/user/domain/PasswordPolicyTest.java`

**Interfaces:**
- Produces: `EmailAddress.normalize(String): String`, `PasswordPolicy.validate(String): void`, enum `PermissionKey` com as dez chaves aprovadas.

- [ ] **Step 1: Escrever testes que quebram ao remover normalização ou limites**

```java
@Test void normalizesWhitespaceAndCase() {
    assertThat(EmailAddress.normalize("  Admin@Glazr.COM ")).isEqualTo("admin@glazr.com");
}

@ParameterizedTest
@ValueSource(ints = {0, 11, 129})
void rejectsPasswordOutsideTwelveToOneHundredTwentyEight(int size) {
    assertThatThrownBy(() -> PasswordPolicy.validate("x".repeat(size)))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=EmailAddressTest,PasswordPolicyTest test`
Expected: FAIL por tipos ausentes.

- [ ] **Step 3: Implementar regras mínimas sem normalizar senha**

```java
public static String normalize(String email) {
    return Objects.requireNonNull(email, "email").strip().toLowerCase(Locale.ROOT);
}

public static void validate(String password) {
    if (password == null || password.length() < 12 || password.length() > 128) {
        throw new IllegalArgumentException("A senha deve ter entre 12 e 128 caracteres");
    }
}
```

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=EmailAddressTest,PasswordPolicyTest test`
Expected: PASS.

### Task 3: Schema Flyway e repositories JPA

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_identity_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__create_spring_session_schema.sql`
- Create: `backend/src/main/resources/db/migration/V3__seed_permission_catalog.sql`
- Create: entidades e repositories em `user/infrastructure`, `accessprofile/infrastructure`, `permission/infrastructure`
- Create: `backend/src/test/java/com/glazr/integration/PostgreSqlIntegrationTest.java`
- Test: `backend/src/test/java/com/glazr/integration/DatabaseMigrationIntegrationTest.java`
- Test: `backend/src/test/java/com/glazr/integration/IdentityRepositoryIntegrationTest.java`

**Interfaces:**
- Produces: `UserRepository`, `AccessProfileRepository`, `PermissionRepository`; schema completo validável por Hibernate.

- [ ] **Step 1: Criar base Testcontainers e testes de migrations/constraints**

```java
@Test void rejectsEmailsThatDifferOnlyByCase() {
    users.saveAndFlush(user("owner@glazr.local"));
    assertThatThrownBy(() -> users.saveAndFlush(user("OWNER@GLAZR.LOCAL")))
        .isInstanceOf(DataIntegrityViolationException.class);
}
```

O teste também consulta `flyway_schema_history`, perfis/permissões e persiste associações.

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=DatabaseMigrationIntegrationTest,IdentityRepositoryIntegrationTest test`
Expected: FAIL por migrations/repositories ausentes (ou Testcontainers indisponível se Docker ainda estiver parado, a registrar sem mascarar).

- [ ] **Step 3: Implementar migrations incrementais e mapeamentos JPA**

```sql
CREATE UNIQUE INDEX uk_users_email_ci ON users (lower(email));
CREATE UNIQUE INDEX uk_access_profiles_name_ci ON access_profiles (lower(name));
ALTER TABLE user_access_profiles
  ADD CONSTRAINT pk_user_access_profiles PRIMARY KEY (user_id, access_profile_id);
```

Usar as tabelas oficiais PostgreSQL de Spring Session e `ON CONFLICT` somente na carga do catálogo/perfil de sistema.

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=DatabaseMigrationIntegrationTest,IdentityRepositoryIntegrationTest test`
Expected: PASS quando Docker estiver disponível.

### Task 4: Bootstrap do administrador

**Files:**
- Create: `backend/src/main/java/com/glazr/configuration/GlazrProperties.java`
- Create: `backend/src/main/java/com/glazr/user/application/AdministratorBootstrapService.java`
- Create: `backend/src/main/java/com/glazr/configuration/AdministratorBootstrapRunner.java`
- Test: `backend/src/test/java/com/glazr/user/application/AdministratorBootstrapServiceTest.java`
- Test: `backend/src/test/java/com/glazr/integration/AdministratorBootstrapIntegrationTest.java`

**Interfaces:**
- Produces: `AdministratorBootstrapService.bootstrap(BootstrapCredentials): BootstrapOutcome`; runner pós-inicialização.

- [ ] **Step 1: Escrever testes para vazio, incompleto e idempotente**

```java
@Test void failsSafelyWhenDatabaseIsEmptyAndCredentialsAreIncomplete() {
    assertThatThrownBy(() -> service.bootstrap(new BootstrapCredentials("admin@x", null, "Admin")))
        .hasMessageContaining("GLAZR_BOOTSTRAP_ADMIN_PASSWORD")
        .hasMessageNotContaining("admin@x");
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=AdministratorBootstrapServiceTest test`
Expected: FAIL por caso de uso ausente.

- [ ] **Step 3: Implementar bootstrap transacional e BCrypt 12**

```java
if (userRepository.count() > 0) return BootstrapOutcome.SKIPPED_EXISTING_USERS;
credentials.validateComplete();
PasswordPolicy.validate(credentials.password());
UserEntity admin = UserEntity.create(..., passwordEncoder.encode(credentials.password()), administrator);
userRepository.save(admin);
return BootstrapOutcome.CREATED;
```

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=AdministratorBootstrapServiceTest,AdministratorBootstrapIntegrationTest test`
Expected: PASS com Docker; unitário sempre PASS.

### Task 5: Bloqueio de login e autenticação por sessão

**Files:**
- Create: classes em `authentication/domain` para `LoginDecision` e `AuthenticatedUser`
- Create: `authentication/application/AuthenticationService.java`
- Create: `authentication/infrastructure/GlazrUserPrincipal.java`
- Create: `authentication/api/AuthenticationController.java` e DTOs
- Test: `authentication/application/AuthenticationServiceTest.java`
- Test: `authentication/api/AuthenticationApiIntegrationTest.java`

**Interfaces:**
- Produces: `AuthenticationService.authenticate(String,String): AuthenticatedUser`; endpoints `/auth/csrf`, `/auth/login`, `/auth/me`, `/auth/logout`.

- [ ] **Step 1: Escrever testes determinísticos de cinco falhas e desbloqueio**

```java
@Test void locksForFifteenMinutesAfterFifthInvalidPassword() {
    IntStream.range(0, 5).forEach(i -> assertThatThrownBy(() -> service.authenticate(EMAIL, "wrong-password"))
        .isInstanceOf(AuthenticationFailedException.class));
    assertThat(user.lockedUntil()).contains(INSTANT.plus(Duration.ofMinutes(15)));
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=AuthenticationServiceTest test`
Expected: FAIL por serviço ausente.

- [ ] **Step 3: Implementar comparação sentinela, lock e sessão**

```java
String candidateHash = user.map(UserEntity::getPasswordHash).orElse(DUMMY_BCRYPT_HASH);
boolean matches = passwordEncoder.matches(password, candidateHash);
if (user.isEmpty() || !matches || !user.get().isActive() || user.get().isLocked(clock.instant())) {
    user.filter(u -> u.isActive() && !u.isLocked(clock.instant())).ifPresent(u -> u.recordFailure(clock.instant()));
    throw new AuthenticationFailedException();
}
```

No controller, trocar ID da sessão, salvar `SecurityContext` explicitamente e retornar DTO seguro.

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=AuthenticationServiceTest,AuthenticationApiIntegrationTest test`
Expected: PASS com Docker para integração.

### Task 6: Security chain, CSRF, CORS e revalidação

**Files:**
- Create: `backend/src/main/java/com/glazr/configuration/SecurityConfiguration.java`
- Create: `backend/src/main/java/com/glazr/authentication/infrastructure/ActiveUserSessionFilter.java`
- Create: `backend/src/main/java/com/glazr/shared/api/SecurityProblemWriter.java`
- Test: `backend/src/test/java/com/glazr/authentication/api/SecurityIntegrationTest.java`

**Interfaces:**
- Produces: filtro que recarrega principal/authorities; Problem Details para 401/403; cookie CSRF SPA.

- [ ] **Step 1: Escrever testes de CSRF, permissão e usuário desativado**

```java
@Test void rejectsMutableRequestWithoutCsrf() throws Exception {
    mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(validLoginJson))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=SecurityIntegrationTest test`
Expected: FAIL porque contrato/configuração não existe.

- [ ] **Step 3: Configurar SecurityFilterChain e filtro ativo**

```java
http.csrf(csrf -> csrf.csrfTokenRepository(cookieCsrfTokenRepository()))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(GET, "/api/v1/auth/csrf", "/actuator/health").permitAll()
        .requestMatchers(POST, "/api/v1/auth/login").permitAll()
        .anyRequest().authenticated());
```

O filtro consulta usuário pelo UUID, invalida sessão inativa e substitui a autenticação pelas authorities atuais.

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=SecurityIntegrationTest test`
Expected: PASS com Docker.

### Task 7: Casos de uso e API de usuários

**Files:**
- Create: `backend/src/main/java/com/glazr/user/application/UserManagementService.java`
- Create: `backend/src/main/java/com/glazr/user/api/UserController.java` e DTOs
- Create: `backend/src/main/java/com/glazr/authentication/infrastructure/UserSessionService.java`
- Test: `backend/src/test/java/com/glazr/user/application/UserManagementServiceTest.java`
- Test: `backend/src/test/java/com/glazr/user/api/UserApiIntegrationTest.java`

**Interfaces:**
- Produces: create/list/get/patch/status; `UserSessionService.deleteSessions(UUID): void`.

- [ ] **Step 1: Escrever testes de criação, perfis e último administrador**

```java
@Test void cannotDeactivateTheLastActiveAdministrator() {
    assertThatThrownBy(() -> service.updateStatus(lastAdminId, false))
        .isInstanceOf(LastActiveAdministratorException.class);
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=UserManagementServiceTest test`
Expected: FAIL por serviço ausente.

- [ ] **Step 3: Implementar casos de uso, autorização e invalidação**

```java
@PreAuthorize("hasAuthority('USER_STATUS_UPDATE')")
@Transactional
public UserResponse updateStatus(UUID id, boolean active) {
    UserEntity user = requireUser(id);
    administratorGuard.assertMayChangeStatus(user, active);
    user.setActive(active, clock.instant());
    if (!active) userSessionService.deleteSessions(id);
    return mapper.toResponse(user);
}
```

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=UserManagementServiceTest,UserApiIntegrationTest test`
Expected: PASS com Docker para integração.

### Task 8: Casos de uso e API de perfis/permissões

**Files:**
- Create: `accessprofile/application/AccessProfileService.java`
- Create: `accessprofile/api/AccessProfileController.java` e DTOs
- Create: `permission/application/PermissionQueryService.java`
- Create: `permission/api/PermissionController.java` e DTOs
- Test: `accessprofile/application/AccessProfileServiceTest.java`
- Test: `accessprofile/api/AccessProfileApiIntegrationTest.java`

**Interfaces:**
- Produces: CRUD de perfis, lista de permissões e conflitos protegidos/em uso.

- [ ] **Step 1: Escrever testes contra mutação/exclusão protegida e chave desconhecida**

```java
@Test void protectedAdministratorCannotLosePermissions() {
    assertThatThrownBy(() -> service.replace(adminId, commandWithPermissions(Set.of("USER_READ"))))
        .isInstanceOf(ProtectedAccessProfileException.class);
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=AccessProfileServiceTest test`
Expected: FAIL por serviço ausente.

- [ ] **Step 3: Implementar validação de catálogo e endpoints `@PreAuthorize`**

```java
Set<PermissionEntity> permissions = permissionRepository.findAllById(command.permissionKeys());
if (permissions.size() != command.permissionKeys().size()) {
    throw new UnknownPermissionException();
}
profile.replace(command.name(), command.description(), command.active(), permissions, now);
```

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=AccessProfileServiceTest,AccessProfileApiIntegrationTest test`
Expected: PASS com Docker para integração.

### Task 9: Problem Details e validação uniforme

**Files:**
- Create: `backend/src/main/java/com/glazr/shared/exception/*`
- Create: `backend/src/main/java/com/glazr/shared/api/ApiExceptionHandler.java`
- Test: `backend/src/test/java/com/glazr/shared/api/ApiExceptionHandlerIntegrationTest.java`

**Interfaces:**
- Produces: `application/problem+json` com código estável e `fieldErrors` seguros.

- [ ] **Step 1: Escrever testes de 400/404/409/500**

```java
@Test void validationUsesProblemDetailAndFieldErrors() throws Exception {
    mvc.perform(post("/api/v1/users").with(authAdmin()).with(csrf())
        .contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors").isArray());
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=ApiExceptionHandlerIntegrationTest test`
Expected: FAIL por formato ausente.

- [ ] **Step 3: Implementar handler sem detalhes internos**

```java
ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, safeDetail);
problem.setTitle(title);
problem.setProperty("code", code);
problem.setProperty("timestamp", clock.instant());
```

- [ ] **Step 4: Executar e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=ApiExceptionHandlerIntegrationTest test`
Expected: PASS com Docker.

### Task 10: Profiles e configuração de runtime

**Files:**
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Test: `backend/src/test/java/com/glazr/configuration/ConfigurationValidationTest.java`

**Interfaces:**
- Produces: propriedades de datasource, sessão, cookie, CORS, Actuator e logs por ambiente.

- [ ] **Step 1: Testar rejeição de origem curinga com credenciais**

```java
@Test void rejectsWildcardAllowedOrigin() {
    assertThatThrownBy(() -> GlazrProperties.allowedOrigins("*"))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Executar RED, implementar profiles e confirmar GREEN**

Run: `backend/mvnw.cmd -f backend/pom.xml -Dtest=ConfigurationValidationTest test`
Expected antes: FAIL; depois: PASS. `application-prod.yml` exige variáveis de banco e cookie Secure.

### Task 11: Containers, Nginx e validação estática

**Files:**
- Create: `backend/Dockerfile`, `backend/.dockerignore`
- Create: `compose.yaml`, `infra/nginx/nginx.conf`
- Create: `.env.example`, `.gitignore`

**Interfaces:**
- Produces: serviços `postgres`, `backend`, `nginx`; entrada HTTP em `${GLAZR_HTTP_PORT:-8080}`.

- [ ] **Step 1: Criar configuração sem segredos e com imagens fixas**

```yaml
services:
  postgres:
    image: postgres:17.11-alpine3.24
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
  backend:
    build: ./backend
    depends_on:
      postgres: { condition: service_healthy }
  nginx:
    image: nginx:1.30.4-alpine3.24
```

- [ ] **Step 2: Validar sintaxe Compose e Nginx**

Run: `docker compose --env-file .env.example config`
Expected: exit 0 e nenhuma tag `latest`.

- [ ] **Step 3: Construir imagens quando daemon estiver disponível**

Run: `docker compose --env-file .env.example build`
Expected: exit 0; runtime contém somente jar e executa como não root.

### Task 12: README, verificação e smoke test

**Files:**
- Create: `README.md`
- Create: `scripts/smoke-test.ps1`
- Modify: documentos caso a implementação divirja de alguma decisão.

**Interfaces:**
- Produces: instruções reproduzíveis e smoke test CSRF/login/perfil/usuário/403/logout.

- [ ] **Step 1: Documentar execução, segurança e remoção destrutiva de volume separadamente**

O README inclui preparação de `.env`, Maven Wrapper com Java 21, Compose, saúde/logs, fluxo CSRF, curl/PowerShell sem senha real, parada sem apagar dados e aviso explícito para `docker compose down -v`.

- [ ] **Step 2: Executar build/teste completo**

Run: `backend/mvnw.cmd -f backend/pom.xml clean verify`
Expected: exit 0 e zero falhas; se Docker estiver parado, registrar exatamente os testes bloqueados.

- [ ] **Step 3: Executar stack e smoke test por Nginx**

Run: `docker compose --env-file .env.example up -d --wait`
Run: `powershell -File scripts/smoke-test.ps1 -BaseUrl http://localhost:8080`
Expected: saúde dos três serviços, login/me/criação/403/logout conforme spec.

- [ ] **Step 4: Inspecionar logs, artefatos e segredos**

Run: `docker compose logs --no-color`
Run: `rg -n -i 'password\s*[:=]\s*[^$<{]' -g '!target/**' -g '!.env.example' .`
Run: `git status --short && git diff --check`
Expected: sem exceções inesperadas, credenciais reais, whitespace errors ou arquivos não documentados.

## Auto-revisão do plano

- Cobertura: os doze incrementos mapeiam build, domínio, migrations, bootstrap, autenticação, segurança, usuários, perfis, erros, configuração, infraestrutura, documentação e verificação.
- Placeholders: não há marcadores pendentes, passos adiados ou referências a código indefinido.
- Consistência: `PermissionKey`, `AuthenticationService`, `UserManagementService`, `AccessProfileService` e `UserSessionService` mantêm nomes e responsabilidades únicos entre tarefas.
- Restrição ambiental conhecida: testes Testcontainers e build/up do Compose dependem de o daemon Docker Desktop voltar a operar; testes unitários e compilação continuam executáveis com `JAVA_HOME` apontando para o JDK 21 encontrado.
