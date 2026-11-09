package com.glazr.integration;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.mail.application.EmployeeMailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthorizationMatrixIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-alpine3.24")
            .withDatabaseName("glazr_authorization_test")
            .withUsername("glazr_test")
            .withPassword("glazr_test_password");

    private static final String MISSING_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ADMIN_PROFILE_ID = "00000000-0000-0000-0000-000000000001";

    private static final Map<String, ExpectedResponse> EXPECTED_RESPONSES = Map.ofEntries(
            expected("csrf token", 200),
            expected("authentication configuration", 200),
            expected("password login", 401, "AUTHENTICATION_FAILED"),
            expected("Google login start", 404, "GOOGLE_AUTH_DISABLED"),
            expected("invitation inspection", 400, "INVITATION_INVALID"),
            expected("password invitation acceptance", 400, "INVITATION_INVALID"),
            expected("Google invitation start", 404, "GOOGLE_AUTH_DISABLED"),
            expected("password reset request", 204),
            expected("password reset completion", 400, "PASSWORD_RESET_INVALID"),
            expected("logout", 204),
            expected("current user", 200),
            expected("password change", 409, "PASSWORD_CREDENTIAL_NOT_CONFIGURED"),
            expected("Google account link", 404, "GOOGLE_AUTH_DISABLED"),
            expected("permission catalog", 200),
            expected("access profile creation", 201),
            expected("access profile listing", 200),
            expected("access profile detail", 404, "RESOURCE_NOT_FOUND"),
            expected("access profile replacement", 404, "RESOURCE_NOT_FOUND"),
            expected("access profile deletion", 404, "RESOURCE_NOT_FOUND"),
            expected("user creation", 201),
            expected("user listing", 200),
            expected("user detail", 404, "RESOURCE_NOT_FOUND"),
            expected("user update without profile assignment", 404, "RESOURCE_NOT_FOUND"),
            expected("user update with profile assignment", 404, "RESOURCE_NOT_FOUND"),
            expected("user status update", 404, "RESOURCE_NOT_FOUND"),
            expected("user invitation detail", 404, "RESOURCE_NOT_FOUND"),
            expected("user invitation resend", 404, "RESOURCE_NOT_FOUND"),
            expected("user invitation revocation", 404, "RESOURCE_NOT_FOUND"),
            expected("kit creation", 201),
            expected("kit listing", 200),
            expected("kit detail", 404, "RESOURCE_NOT_FOUND"),
            expected("kit replacement", 404, "RESOURCE_NOT_FOUND"),
            expected("kit deletion", 404, "RESOURCE_NOT_FOUND"),
            expected("glass creation", 201),
            expected("glass listing", 200),
            expected("glass detail", 404, "RESOURCE_NOT_FOUND"),
            expected("glass update", 404, "RESOURCE_NOT_FOUND"),
            expected("glass status update", 404, "RESOURCE_NOT_FOUND"),
            expected("glass deletion", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory listing", 200),
            expected("inventory alerts", 200),
            expected("inventory summary", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory sheet listing", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory movement listing", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory minimum update", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory sheet receipt", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory sheet adjustment", 404, "RESOURCE_NOT_FOUND"),
            expected("inventory sheet cut", 404, "RESOURCE_NOT_FOUND"),
            expected("quote creation", 201),
            expected("quote listing", 200),
            expected("quote detail", 404, "RESOURCE_NOT_FOUND"),
            expected("quote update", 404, "RESOURCE_NOT_FOUND"),
            expected("quote status update", 404, "RESOURCE_NOT_FOUND"),
            expected("quote deletion", 404, "RESOURCE_NOT_FOUND")
    );

    private static final List<EndpointCase> ENDPOINTS = List.of(
            publicEndpoint("csrf token", HttpMethod.GET, "/api/v1/auth/csrf", null),
            publicEndpoint("authentication configuration", HttpMethod.GET, "/api/v1/auth/config", null),
            publicEndpoint("password login", HttpMethod.POST, "/api/v1/auth/login",
                    "{\"email\":\"unknown@authorization.invalid\",\"password\":\"senha incorreta longa\"}"),
            publicEndpoint("Google login start", HttpMethod.POST, "/api/v1/auth/google/start", null),
            publicEndpoint("invitation inspection", HttpMethod.POST, "/api/v1/invitations/inspect",
                    "{\"token\":\"invalid-token\"}"),
            publicEndpoint("password invitation acceptance", HttpMethod.POST,
                    "/api/v1/invitations/accept-with-password",
                    "{\"token\":\"invalid-token\",\"password\":\"uma frase senha segura\"}"),
            publicEndpoint("Google invitation start", HttpMethod.POST, "/api/v1/invitations/google/start",
                    "{\"token\":\"invalid-token\"}"),
            publicEndpoint("password reset request", HttpMethod.POST, "/api/v1/auth/password-reset/request",
                    "{\"email\":\"unknown@authorization.invalid\"}"),
            publicEndpoint("password reset completion", HttpMethod.POST, "/api/v1/auth/password-reset/complete",
                    "{\"token\":\"invalid-token\",\"newPassword\":\"uma frase senha segura\"}"),
            publicFilterEndpoint("logout", HttpMethod.POST, "/api/v1/auth/logout", null),

            authenticatedEndpoint("current user", HttpMethod.GET, "/api/v1/auth/me", null),
            authenticatedEndpoint("password change", HttpMethod.POST, "/api/v1/auth/password/change",
                    "{\"currentPassword\":\"senha atual segura\",\"newPassword\":\"nova frase senha segura\"}"),
            authenticatedEndpoint("Google account link", HttpMethod.POST, "/api/v1/auth/google/link/start", null),

            permissionEndpoint("permission catalog", HttpMethod.GET, "/api/v1/permissions", null,
                    "PERMISSION_READ"),

            permissionEndpoint("access profile creation", HttpMethod.POST, "/api/v1/access-profiles",
                    "{\"name\":\"MATRIZ_AUTORIZACAO\",\"description\":\"Teste\",\"active\":true,"
                            + "\"permissionKeys\":[\"USER_READ\"]}",
                    "ACCESS_PROFILE_CREATE"),
            permissionEndpoint("access profile listing", HttpMethod.GET, "/api/v1/access-profiles", null,
                    "ACCESS_PROFILE_READ"),
            permissionEndpoint("access profile detail", HttpMethod.GET,
                    "/api/v1/access-profiles/" + MISSING_ID, null, "ACCESS_PROFILE_READ"),
            permissionEndpoint("access profile replacement", HttpMethod.PUT,
                    "/api/v1/access-profiles/" + MISSING_ID,
                    "{\"name\":\"MATRIZ_ATUALIZADA\",\"description\":\"Teste\",\"active\":true,"
                            + "\"permissionKeys\":[\"USER_READ\"]}",
                    "ACCESS_PROFILE_UPDATE"),
            permissionEndpoint("access profile deletion", HttpMethod.DELETE,
                    "/api/v1/access-profiles/" + MISSING_ID, null, "ACCESS_PROFILE_DELETE"),

            permissionEndpoint("user creation", HttpMethod.POST, "/api/v1/users",
                    "{\"email\":\"matrix-user@authorization.invalid\",\"displayName\":\"Usuário Matriz\","
                            + "\"accessProfileIds\":[\"" + ADMIN_PROFILE_ID + "\"]}",
                    "USER_CREATE", "ACCESS_PROFILE_ASSIGN"),
            permissionEndpoint("user listing", HttpMethod.GET, "/api/v1/users", null, "USER_READ"),
            permissionEndpoint("user detail", HttpMethod.GET, "/api/v1/users/" + MISSING_ID, null,
                    "USER_READ"),
            permissionEndpoint("user update without profile assignment", HttpMethod.PATCH,
                    "/api/v1/users/" + MISSING_ID, "{\"displayName\":\"Usuário Atualizado\"}",
                    "USER_UPDATE"),
            permissionEndpoint("user update with profile assignment", HttpMethod.PATCH,
                    "/api/v1/users/" + MISSING_ID,
                    "{\"accessProfileIds\":[\"" + ADMIN_PROFILE_ID + "\"]}",
                    "USER_UPDATE", "ACCESS_PROFILE_ASSIGN"),
            permissionEndpoint("user status update", HttpMethod.PATCH,
                    "/api/v1/users/" + MISSING_ID + "/status", "{\"active\":false}",
                    "USER_STATUS_UPDATE"),
            permissionEndpoint("user invitation detail", HttpMethod.GET,
                    "/api/v1/users/" + MISSING_ID + "/invitation", null, "USER_READ"),
            permissionEndpoint("user invitation resend", HttpMethod.POST,
                    "/api/v1/users/" + MISSING_ID + "/invitation/resend", null, "USER_UPDATE"),
            permissionEndpoint("user invitation revocation", HttpMethod.POST,
                    "/api/v1/users/" + MISSING_ID + "/invitation/revoke", null, "USER_UPDATE"),

            permissionEndpoint("kit creation", HttpMethod.POST, "/api/v1/kits",
                    "{\"name\":\"Kit Matriz\",\"color\":\"Incolor\",\"price\":100.00,"
                            + "\"components\":[{\"name\":\"Vidro\",\"quantity\":1,\"notes\":null}]}",
                    "KIT_CREATE"),
            permissionEndpoint("kit listing", HttpMethod.GET, "/api/v1/kits", null, "KIT_READ"),
            permissionEndpoint("kit detail", HttpMethod.GET, "/api/v1/kits/" + MISSING_ID, null,
                    "KIT_READ"),
            permissionEndpoint("kit replacement", HttpMethod.PUT, "/api/v1/kits/" + MISSING_ID,
                    "{\"name\":\"Kit Atualizado\",\"color\":\"Fumê\",\"price\":120.00,"
                            + "\"components\":[{\"name\":\"Vidro\",\"quantity\":1,\"notes\":null}]}",
                    "KIT_UPDATE"),
            permissionEndpoint("kit deletion", HttpMethod.DELETE, "/api/v1/kits/" + MISSING_ID, null,
                    "KIT_DELETE"),

            permissionEndpoint("glass creation", HttpMethod.POST, "/api/v1/glasses",
                    "{\"name\":\"Vidro Matriz\",\"thicknessMm\":8.00,\"finish\":\"Lapidado\","
                            + "\"color\":\"Incolor\",\"pricePerSquareMeter\":180.00}",
                    "GLASS_CREATE"),
            permissionEndpoint("glass listing", HttpMethod.GET, "/api/v1/glasses", null, "GLASS_READ"),
            permissionEndpoint("glass detail", HttpMethod.GET, "/api/v1/glasses/" + MISSING_ID, null,
                    "GLASS_READ"),
            permissionEndpoint("glass update", HttpMethod.PATCH, "/api/v1/glasses/" + MISSING_ID,
                    "{\"name\":\"Vidro Atualizado\",\"thicknessMm\":8.00,\"finish\":\"Lapidado\","
                            + "\"color\":\"Incolor\",\"pricePerSquareMeter\":190.00}",
                    "GLASS_UPDATE"),
            permissionEndpoint("glass status update", HttpMethod.PATCH,
                    "/api/v1/glasses/" + MISSING_ID + "/status", "{\"active\":false}",
                    "GLASS_UPDATE"),
            permissionEndpoint("glass deletion", HttpMethod.DELETE, "/api/v1/glasses/" + MISSING_ID, null,
                    "GLASS_DELETE"),

            permissionEndpoint("inventory listing", HttpMethod.GET, "/api/v1/inventory/glasses", null,
                    "GLASS_READ"),
            permissionEndpoint("inventory alerts", HttpMethod.GET, "/api/v1/inventory/glasses/alerts", null,
                    "GLASS_READ"),
            permissionEndpoint("inventory summary", HttpMethod.GET,
                    "/api/v1/inventory/glasses/" + MISSING_ID, null, "GLASS_READ"),
            permissionEndpoint("inventory sheet listing", HttpMethod.GET,
                    "/api/v1/inventory/glasses/" + MISSING_ID + "/sheets", null, "GLASS_READ"),
            permissionEndpoint("inventory movement listing", HttpMethod.GET,
                    "/api/v1/inventory/glasses/" + MISSING_ID + "/movements", null, "GLASS_READ"),
            permissionEndpoint("inventory minimum update", HttpMethod.PUT,
                    "/api/v1/inventory/glasses/" + MISSING_ID + "/minimum",
                    "{\"minimumAreaM2\":1.000000}", "GLASS_UPDATE"),
            permissionEndpoint("inventory sheet receipt", HttpMethod.POST,
                    "/api/v1/inventory/glasses/" + MISSING_ID + "/receipts",
                    "{\"widthMm\":2000.00,\"heightMm\":3000.00,\"quantity\":1,\"notes\":\"Teste\"}",
                    "GLASS_UPDATE"),
            permissionEndpoint("inventory sheet adjustment", HttpMethod.PATCH,
                    "/api/v1/inventory/sheets/" + MISSING_ID + "/adjustment",
                    "{\"remainingAreaM2\":1.000000,\"notes\":\"Ajuste de teste\"}",
                    "GLASS_UPDATE"),
            permissionEndpoint("inventory sheet cut", HttpMethod.POST,
                    "/api/v1/inventory/sheets/" + MISSING_ID + "/cuts",
                    "{\"pieceWidthMm\":500.00,\"pieceHeightMm\":500.00,"
                            + "\"usableRemainderAreaM2\":1.000000,\"notes\":\"Corte de teste\"}",
                    "GLASS_UPDATE"),

            permissionEndpoint("quote creation", HttpMethod.POST, "/api/v1/quotes",
                    "{\"customerName\":\"Cliente Matriz\",\"customerPhone\":\"31999999999\","
                            + "\"customerEmail\":\"cliente@teste.com\",\"customerAddress\":\"Rua Teste, 100\","
                            + "\"laborCost\":50.00,\"freightCost\":30.00,\"glassItems\":[],\"kitItems\":[]}",
                    "QUOTE_CREATE"),
            permissionEndpoint("quote listing", HttpMethod.GET, "/api/v1/quotes", null, "QUOTE_READ"),
            permissionEndpoint("quote detail", HttpMethod.GET, "/api/v1/quotes/" + MISSING_ID, null, "QUOTE_READ"),
            permissionEndpoint("quote update", HttpMethod.PUT, "/api/v1/quotes/" + MISSING_ID,
                    "{\"customerName\":\"Cliente Atualizado\",\"customerPhone\":\"31999999999\","
                            + "\"customerEmail\":\"cliente@teste.com\",\"customerAddress\":\"Rua Teste, 100\","
                            + "\"laborCost\":60.00,\"freightCost\":35.00,\"glassItems\":[],\"kitItems\":[]}",
                    "QUOTE_UPDATE"),
            permissionEndpoint("quote status update", HttpMethod.PATCH, "/api/v1/quotes/" + MISSING_ID + "/status",
                    "{\"status\":\"APPROVED\"}", "QUOTE_STATUS_UPDATE"),
            permissionEndpoint("quote deletion", HttpMethod.DELETE, "/api/v1/quotes/" + MISSING_ID, null, "QUOTE_DELETE")
    );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("glazr.bootstrap.admin.email", () -> "admin@authorization.glazr");
        registry.add("glazr.bootstrap.admin.password", () -> "frase senha autorização");
        registry.add("glazr.bootstrap.admin.name", () -> "Administrador de Autorização");
        registry.add("glazr.google-auth-enabled", () -> "false");
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    EmployeeMailSender mailSender;

    @Test
    void matrixCoversEveryApiControllerEndpoint() {
        Set<String> registered = mappings.getHandlerMethods().entrySet().stream()
                .filter(entry -> isGlazrApiController(entry.getValue()))
                .flatMap(entry -> routeKeys(entry.getKey()).stream())
                .collect(Collectors.toSet());
        Set<String> catalogued = ENDPOINTS.stream()
                .filter(EndpointCase::controllerMapping)
                .map(EndpointCase::key)
                .collect(Collectors.toSet());

        assertThat(catalogued).containsExactlyInAnyOrderElementsOf(registered);
        assertThat(EXPECTED_RESPONSES.keySet()).containsExactlyInAnyOrderElementsOf(
                ENDPOINTS.stream().map(EndpointCase::name).toList());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void anonymousCallerMatchesTheDeclaredAccessLevel(EndpointCase endpoint) throws Exception {
        var result = mockMvc.perform(requestFor(endpoint));

        if (endpoint.access() == AccessLevel.PUBLIC) {
            result.andExpect(expectedResponse(endpoint));
        } else {
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void authenticatedCallerWithoutPermissionsMatchesTheDeclaredAccessLevel(EndpointCase endpoint) throws Exception {
        var result = mockMvc.perform(requestFor(endpoint).with(authenticationFor(Set.of())));

        if (endpoint.access() == AccessLevel.PERMISSION) {
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        } else {
            result.andExpect(expectedResponse(endpoint));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void callerWithTheExactRequiredAuthoritiesCrossesTheSecurityBoundary(EndpointCase endpoint) throws Exception {
        mockMvc.perform(requestFor(endpoint).with(authenticationFor(endpoint.authorities())))
                .andExpect(expectedResponse(endpoint));
    }

    @ParameterizedTest(name = "{0}; missing {1}")
    @MethodSource("casesMissingOneRequiredAuthority")
    void callerMissingAnyOneRequiredAuthorityIsDenied(EndpointCase endpoint, String missingAuthority,
                                                       Set<String> grantedAuthorities) throws Exception {
        mockMvc.perform(requestFor(endpoint).with(authenticationFor(grantedAuthorities)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    static Stream<EndpointCase> endpoints() {
        return ENDPOINTS.stream();
    }

    static Stream<Arguments> casesMissingOneRequiredAuthority() {
        return ENDPOINTS.stream()
                .filter(endpoint -> endpoint.access() == AccessLevel.PERMISSION)
                .flatMap(endpoint -> endpoint.authorities().stream().map(missingAuthority -> {
                    Set<String> granted = endpoint.authorities().stream()
                            .filter(authority -> !authority.equals(missingAuthority))
                            .collect(Collectors.toUnmodifiableSet());
                    return Arguments.of(endpoint, missingAuthority, granted);
                }));
    }

    private MockHttpServletRequestBuilder requestFor(EndpointCase endpoint) {
        MockHttpServletRequestBuilder request = request(endpoint.method(), endpoint.path()).with(csrf());
        if (endpoint.body() != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
        }
        return request;
    }

    private RequestPostProcessor authenticationFor(Set<String> authorities) {
        List<String> sortedAuthorities = authorities.stream().sorted().toList();
        String authorityKey = sortedAuthorities.isEmpty() ? "none" : String.join("-", sortedAuthorities);
        UUID userId = stableId("authorization-user-" + authorityKey);
        String compactId = userId.toString().replace("-", "");

        jdbc.update("""
                insert into users
                    (id, email, display_name, account_type, active, failed_login_attempts,
                     created_at, updated_at, version)
                values (?, ?, ?, 'EMPLOYEE', true, 0, current_timestamp, current_timestamp, 0)
                on conflict (id) do nothing
                """, userId, "authorization-" + compactId + "@example.invalid", "Usuário de autorização");

        if (!sortedAuthorities.isEmpty()) {
            UUID profileId = stableId("authorization-profile-" + authorityKey);
            jdbc.update("""
                    insert into access_profiles
                        (id, name, description, active, system_profile, created_at, updated_at, version)
                    values (?, ?, 'Perfil criado pelo teste da matriz', true, false,
                            current_timestamp, current_timestamp, 0)
                    on conflict (id) do nothing
                    """, profileId, "AUTH_" + compactId.substring(0, 20));
            sortedAuthorities.forEach(authority -> jdbc.update("""
                    insert into access_profile_permissions (access_profile_id, permission_key)
                    values (?, ?)
                    on conflict do nothing
                    """, profileId, authority));
            jdbc.update("""
                    insert into user_access_profiles (user_id, access_profile_id)
                    values (?, ?)
                    on conflict do nothing
                    """, userId, profileId);
        }

        var user = new AuthenticatedUser(userId, "authorization-" + compactId + "@example.invalid",
                "Usuário de autorização", true, "EMPLOYEE", List.of(), sortedAuthorities, List.of());
        var principal = new GlazrUserPrincipal(user);
        var grantedAuthorities = sortedAuthorities.stream().map(SimpleGrantedAuthority::new).toList();
        var token = UsernamePasswordAuthenticationToken.authenticated(principal, null, grantedAuthorities);
        return authentication(token);
    }

    private UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.ResultMatcher expectedResponse(EndpointCase endpoint) {
        ExpectedResponse expected = EXPECTED_RESPONSES.get(endpoint.name());
        assertThat(expected).as("Expected response for %s", endpoint).isNotNull();
        return result -> {
            status().is(expected.status()).match(result);
            if (expected.code() != null) {
                jsonPath("$.code").value(expected.code()).match(result);
            } else if (expected.status() != 204) {
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
                jsonPath("$.code").doesNotExist().match(result);
            } else {
                assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
            }
        };
    }

    private static Map.Entry<String, ExpectedResponse> expected(String name, int status) {
        return expected(name, status, null);
    }

    private static Map.Entry<String, ExpectedResponse> expected(String name, int status, String code) {
        return Map.entry(name, new ExpectedResponse(status, code));
    }

    private record ExpectedResponse(int status, String code) { }

    private boolean isGlazrApiController(HandlerMethod handler) {
        return handler.getBeanType().getPackageName().startsWith("com.glazr")
                && handler.getMethod().getDeclaringClass().getSimpleName().endsWith("Controller");
    }

    private Set<String> routeKeys(RequestMappingInfo mapping) {
        return mapping.getMethodsCondition().getMethods().stream()
                .flatMap(method -> mapping.getPatternValues().stream()
                        .map(path -> method.name() + " " + path))
                .collect(Collectors.toSet());
    }

    private static EndpointCase publicEndpoint(String name, HttpMethod method, String path, String body) {
        return new EndpointCase(name, method, path, body, AccessLevel.PUBLIC, Set.of(), true);
    }

    private static EndpointCase publicFilterEndpoint(String name, HttpMethod method, String path, String body) {
        return new EndpointCase(name, method, path, body, AccessLevel.PUBLIC, Set.of(), false);
    }

    private static EndpointCase authenticatedEndpoint(String name, HttpMethod method, String path, String body) {
        return new EndpointCase(name, method, path, body, AccessLevel.AUTHENTICATED, Set.of(), true);
    }

    private static EndpointCase permissionEndpoint(String name, HttpMethod method, String path, String body,
                                                   String... authorities) {
        return new EndpointCase(name, method, path, body, AccessLevel.PERMISSION, Set.of(authorities), true);
    }

    private enum AccessLevel {
        PUBLIC,
        AUTHENTICATED,
        PERMISSION
    }

    private record EndpointCase(String name, HttpMethod method, String path, String body,
                                AccessLevel access, Set<String> authorities, boolean controllerMapping) {

        String key() {
            return method.name() + " " + path.replace(MISSING_ID, "{id}")
                    .replace("/inventory/glasses/{id}/", "/inventory/glasses/{glassId}/")
                    .replace("/inventory/glasses/{id}", "/inventory/glasses/{glassId}")
                    .replace("/inventory/sheets/{id}/", "/inventory/sheets/{sheetId}/");
        }

        @Override
        public String toString() {
            return name + " [" + method + " " + path + "]";
        }
    }
}
