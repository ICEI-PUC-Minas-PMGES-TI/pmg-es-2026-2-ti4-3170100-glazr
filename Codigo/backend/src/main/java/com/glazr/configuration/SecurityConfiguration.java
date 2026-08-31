package com.glazr.configuration;

import com.glazr.authentication.infrastructure.ActiveUserSessionFilter;
import com.glazr.shared.api.SecurityProblemWriter;
import com.glazr.google.infrastructure.GoogleAuthenticationFailureHandler;
import com.glazr.google.infrastructure.GoogleAuthenticationSuccessHandler;
import com.glazr.google.infrastructure.DiscardingOAuth2AuthorizedClientRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    PasswordEncoder passwordEncoder() { return new PreHashedBCryptPasswordEncoder(12); }

    @Bean
    SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    OAuth2AuthorizedClientRepository oauth2AuthorizedClientRepository() {
        return new DiscardingOAuth2AuthorizedClientRepository();
    }

    @Bean
    CookieSerializer cookieSerializer(@Value("${glazr.cookie.secure:false}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("GLAZR_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite("Lax");
        return serializer;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${glazr.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::strip).filter(value -> !value.isEmpty()).toList();
        if (origins.contains("*")) throw new IllegalArgumentException("Origem CORS curinga não é permitida");
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(!origins.isEmpty());
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ActiveUserSessionFilter activeUserFilter,
                                            SecurityContextRepository contextRepository,
                                            SecurityProblemWriter problems,
                                            GoogleAuthenticationSuccessHandler googleSuccess,
                                            GoogleAuthenticationFailureHandler googleFailure,
                                            OAuth2AuthorizedClientRepository authorizedClients,
                                            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource,
                                            @Value("${glazr.cookie.secure:false}") boolean secure,
                                            @Value("${glazr.google-auth-enabled:false}") boolean googleEnabled) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(secure));

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .cors(cors -> cors.configurationSource(corsSource))
                .securityContext(context -> context
                        .securityContextRepository(contextRepository)
                        .requireExplicitSave(true))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf", "/api/v1/auth/config",
                                "/actuator/health", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/google/start").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations/inspect",
                                "/api/v1/invitations/accept-with-password",
                                "/api/v1/invitations/google/start",
                                "/api/v1/auth/password-reset/request",
                                "/api/v1/auth/password-reset/complete").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> problems.write(
                                request, response, HttpStatus.UNAUTHORIZED,
                                "AUTHENTICATION_REQUIRED", "Autenticação necessária"))
                        .accessDeniedHandler((request, response, exception) -> {
                            String code = exception instanceof MissingCsrfTokenException
                                    || exception instanceof InvalidCsrfTokenException
                                    ? "INVALID_CSRF_TOKEN" : "ACCESS_DENIED";
                            problems.write(request, response, HttpStatus.FORBIDDEN, code, "Acesso negado");
                        }))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("GLAZR_SESSION")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .addFilterBefore(activeUserFilter, AuthorizationFilter.class);
        if (googleEnabled) {
            http.oauth2Login(oauth -> oauth
                    .authorizedClientRepository(authorizedClients)
                    .successHandler(googleSuccess)
                    .failureHandler(googleFailure));
        }
        return http.build();
    }
}
