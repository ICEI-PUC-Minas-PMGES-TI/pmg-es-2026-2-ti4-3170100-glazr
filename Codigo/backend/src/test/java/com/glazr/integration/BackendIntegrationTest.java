package com.glazr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glazr.configuration.BootstrapCredentials;
import com.glazr.authentication.application.AuthenticationFailedException;
import com.glazr.authentication.application.AuthenticationService;
import com.glazr.user.application.AdministratorBootstrapService;
import com.glazr.user.application.LastActiveAdministratorException;
import com.glazr.user.application.UserManagementService;
import com.glazr.mail.application.EmployeeInvitationMessage;
import com.glazr.mail.application.EmployeeMailSender;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BackendIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@integration.glazr";
    private static final String ADMIN_PASSWORD = "frase senha integração";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-alpine3.24")
            .withDatabaseName("glazr_test")
            .withUsername("glazr_test")
            .withPassword("glazr_test_password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("glazr.bootstrap.admin.email", () -> ADMIN_EMAIL);
        registry.add("glazr.bootstrap.admin.password", () -> ADMIN_PASSWORD);
        registry.add("glazr.bootstrap.admin.name", () -> "Administrador de Integração");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AdministratorBootstrapService bootstrap;
    @Autowired AuthenticationService authentication;
    @Autowired UserManagementService userManagement;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EmployeeMailSender mailSender;

    @BeforeEach
    void resetAdministratorLoginState() {
        jdbc.update("""
                update users set active = true, failed_login_attempts = 0, locked_until = null
                where email = ?
                """, ADMIN_EMAIL);
    }

    @Test
    void migrationsCreateSchemaAndSeedCompleteCatalog() {
        Integer migrations = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        Integer permissions = jdbc.queryForObject("select count(*) from permissions", Integer.class);
        Integer adminPermissions = jdbc.queryForObject("""
                select count(*) from access_profile_permissions
                where access_profile_id = '00000000-0000-0000-0000-000000000001'
                """, Integer.class);

        assertThat(migrations).isEqualTo(8);
        assertThat(permissions).isEqualTo(18);
        assertThat(adminPermissions).isEqualTo(18);
    }

    @Test
    void databaseEnforcesCaseInsensitiveEmailUniqueness() {
        String first = "case-" + UUID.randomUUID() + "@glazr.local";
        insertUser(first);
        assertThatThrownBy(() -> insertUser(first.toUpperCase()))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
    }

    @Test
    void administratorBootstrapIsIdempotent() {
        Long before = jdbc.queryForObject("select count(*) from users", Long.class);
        var outcome = bootstrap.bootstrap(() ->
                new BootstrapCredentials("ignored@example.invalid", "senha ignorada segura", "Ignorado"));
        Long after = jdbc.queryForObject("select count(*) from users", Long.class);

        assertThat(outcome).isEqualTo(AdministratorBootstrapService.Outcome.SKIPPED_EXISTING_USERS);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void mutableLoginRequiresCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }

    @Test
    void malformedJsonUsesProblemDetailsContract() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void malformedPathIdentifierUsesProblemDetailsContract() throws Exception {
        Cookie adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(get("/api/v1/users/not-a-uuid").cookie(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void validLoginPersistsSessionAndLogoutInvalidatesIt() throws Exception {
        Cookie session = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.permissions.length()").value(18));
        assertThat(jdbc.queryForObject("select count(*) from spring_session", Integer.class)).isPositive();

        mvc.perform(post("/api/v1/auth/logout").cookie(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void unknownEmailAndWrongPasswordHaveIndistinguishableContract() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@glazr.local", "senha errada longa")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));

        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ADMIN_EMAIL, "senha errada longa")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void concurrentInvalidLoginsAtomicallyReachTheLockThreshold() throws Exception {
        String email = "concurrent-login-" + UUID.randomUUID() + "@glazr.local";
        insertUser(email, passwordEncoder.encode("senha correta concorrente"));
        CyclicBarrier barrier = new CyclicBarrier(5);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<Future<Class<?>>> results = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(ignored -> executor.submit(() -> {
                        barrier.await();
                        try {
                            authentication.authenticate(email, "senha incorreta concorrente");
                            return Void.class;
                        } catch (RuntimeException exception) {
                            return exception.getClass();
                        }
                    }))
                    .toList();

            for (Future<Class<?>> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo(AuthenticationFailedException.class);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertThat(jdbc.queryForObject(
                "select failed_login_attempts from users where email = ?", Integer.class, email)).isEqualTo(5);
        assertThat(jdbc.queryForObject(
                "select locked_until is not null from users where email = ?", Boolean.class, email)).isTrue();
    }

    @Test
    void concurrentAdministratorDeactivationAlwaysLeavesOneActive() throws Exception {
        UUID originalId = jdbc.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);
        jdbc.update("""
                update users set active = false where id in (
                    select up.user_id from user_access_profiles up
                    where up.access_profile_id = '00000000-0000-0000-0000-000000000001'
                      and up.user_id <> ?
                )
                """, originalId);
        String secondEmail = "second-admin-" + UUID.randomUUID() + "@glazr.local";
        UUID secondId = insertUser(secondEmail, passwordEncoder.encode("senha segundo administrador"));
        jdbc.update("""
                insert into user_access_profiles (user_id, access_profile_id)
                values (?, '00000000-0000-0000-0000-000000000001')
                """, secondId);
        CyclicBarrier barrier = new CyclicBarrier(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Class<?>>> results = List.of(originalId, secondId).stream()
                    .map(id -> executor.submit(() -> {
                        SecurityContextHolder.getContext().setAuthentication(
                                new TestingAuthenticationToken("test", "n/a", "USER_STATUS_UPDATE"));
                        barrier.await();
                        try {
                            userManagement.updateStatus(id, false);
                            return Void.class;
                        } catch (RuntimeException exception) {
                            return exception.getClass();
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }))
                    .toList();

            assertThat(results.stream().map(result -> {
                try { return result.get(10, TimeUnit.SECONDS).getSimpleName(); }
                catch (Exception exception) { throw new AssertionError(exception); }
            }).toList()).containsExactlyInAnyOrder(
                    Void.class.getSimpleName(), LastActiveAdministratorException.class.getSimpleName());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            jdbc.update("update users set active = true where id = ?", originalId);
            jdbc.update("delete from user_access_profiles where user_id = ?", secondId);
            jdbc.update("delete from users where id = ?", secondId);
        }

        assertThat(jdbc.queryForObject("""
                select count(*) from users u join user_access_profiles up on up.user_id = u.id
                where u.active and up.access_profile_id = '00000000-0000-0000-0000-000000000001'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void administratorCreatesUserAndDeactivationRevokesTheExistingSession() throws Exception {
        Cookie adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult profileResult = mvc.perform(post("/api/v1/access-profiles")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"OPERADOR_%s","description":"Integração","active":true,
                                 "permissionKeys":["USER_READ"]}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode profile = objectMapper.readTree(profileResult.getResponse().getContentAsByteArray());

        String operatorEmail = "operator-" + suffix + "@glazr.local";
        String operatorPassword = "frase senha operador";
        MvcResult userResult = mvc.perform(post("/api/v1/users")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"Operador",
                                 "accessProfileIds":["%s"]}
                                """.formatted(operatorEmail, profile.get("id").asText())))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode operator = objectMapper.readTree(userResult.getResponse().getContentAsByteArray());

        ArgumentCaptor<EmployeeInvitationMessage> invitationMessage =
                ArgumentCaptor.forClass(EmployeeInvitationMessage.class);
        verify(mailSender).sendInvitation(invitationMessage.capture());
        MvcResult activation = mvc.perform(post("/api/v1/invitations/accept-with-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"%s"}
                                """.formatted(invitationMessage.getValue().rawToken(), operatorPassword)))
                .andExpect(status().isOk()).andReturn();
        Cookie operatorSession = activation.getResponse().getCookie("GLAZR_SESSION");
        assertThat(operatorSession).isNotNull();
        mvc.perform(get("/api/v1/permissions").cookie(operatorSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mvc.perform(patch("/api/v1/users/{id}/status", operator.get("id").asText())
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").cookie(operatorSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void lastActiveAdministratorCannotBeDeactivated() throws Exception {
        Cookie adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        UUID adminId = jdbc.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);

        mvc.perform(patch("/api/v1/users/{id}/status", adminId)
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMINISTRATOR"));
    }

    @Test
    void administratorCreatesKitWithComponentsAndCascadesOnDelete() throws Exception {
        Cookie adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mvc.perform(post("/api/v1/kits")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Kit Box %s","color":"Incolor","price":1250.00,
                                 "components":[{"name":"Vidro 8mm","quantity":2,"notes":"opcional"}]}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kit Box " + suffix))
                .andExpect(jsonPath("$.color").value("Incolor"))
                .andExpect(jsonPath("$.components.length()").value(1))
                .andExpect(jsonPath("$.components[0].name").value("Vidro 8mm"))
                .andExpect(jsonPath("$.components[0].quantity").value(2))
                .andReturn();
        JsonNode kit = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        UUID kitId = UUID.fromString(kit.get("id").asText());

        mvc.perform(post("/api/v1/kits")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"kit box %s","color":"Incolor","price":10.00,
                                 "components":[{"name":"Vidro","quantity":1,"notes":null}]}
                                """.formatted(suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_KIT"));

        mvc.perform(delete("/api/v1/kits/{id}", kitId)
                        .cookie(adminSession).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject(
                "select count(*) from kit_components where kit_id = ?", Integer.class, kitId)).isZero();
    }

    @Test
    void administratorReplacesKitAndSwapsItsComponents() throws Exception {
        Cookie adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mvc.perform(post("/api/v1/kits")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Kit Original %s","color":"Incolor","price":100.00,
                                 "components":[{"name":"Vidro 6mm","quantity":1,"notes":null}]}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode kit = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        UUID kitId = UUID.fromString(kit.get("id").asText());

        mvc.perform(put("/api/v1/kits/{id}", kitId)
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Kit Atualizado %s","color":"Fumê","price":200.50,
                                 "components":[{"name":"Vidro 8mm","quantity":2,"notes":"novo"},
                                               {"name":"Ferragem","quantity":1,"notes":null}]}
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kit Atualizado " + suffix))
                .andExpect(jsonPath("$.components.length()").value(2))
                .andExpect(jsonPath("$.components[0].name").value("Ferragem"))
                .andExpect(jsonPath("$.components[1].name").value("Vidro 8mm"));

        assertThat(jdbc.queryForObject(
                "select count(*) from kit_components where kit_id = ?", Integer.class, kitId)).isEqualTo(2);

        mvc.perform(delete("/api/v1/kits/{id}", kitId).cookie(adminSession).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/kits/{id}", kitId).cookie(adminSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void administratorCreatesAndManagesGlasses() throws Exception {
        Cookie adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mvc.perform(post("/api/v1/glasses")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vidro Teste %s","thicknessMm":8.00,"finish":"Lapidado","color":"Incolor","pricePerSquareMeter":175.50}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vidro Teste " + suffix))
                .andExpect(jsonPath("$.thicknessMm").value(8.0))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        JsonNode glass = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        UUID glassId = UUID.fromString(glass.get("id").asText());

        mvc.perform(post("/api/v1/glasses")
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vidro Teste %s","thicknessMm":8.00,"finish":"Lapidado","color":"Incolor","pricePerSquareMeter":200.00}
                                """.formatted(suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_GLASS"));

        mvc.perform(patch("/api/v1/glasses/{id}/status", glassId)
                        .cookie(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete("/api/v1/glasses/{id}", glassId).cookie(adminSession).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private Cookie login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie sessionCookie = result.getResponse().getCookie("GLAZR_SESSION");
        assertThat(sessionCookie).isNotNull();
        return sessionCookie;
    }

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private void insertUser(String email) {
        insertUser(email, "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW");
    }

    private UUID insertUser(String email, String passwordHash) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into users
                    (id, email, display_name, account_type, active, failed_login_attempts, created_at, updated_at, version)
                values (?, ?, 'Teste', 'EMPLOYEE', true, 0,
                        current_timestamp, current_timestamp, 0)
                """, id, email);
        jdbc.update("""
                insert into user_password_credentials
                    (user_id, password_hash, password_changed_at, created_at, updated_at)
                values (?, ?, current_timestamp, current_timestamp, current_timestamp)
                """, id, passwordHash);
        return id;
    }
}
