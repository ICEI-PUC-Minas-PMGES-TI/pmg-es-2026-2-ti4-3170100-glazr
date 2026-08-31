package com.glazr.google.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class GoogleOidcConfiguration {
    @Bean
    @ConditionalOnProperty(name = "glazr.google-auth-enabled", havingValue = "true")
    ClientRegistrationRepository googleClientRegistration(
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (clientId.isBlank() || clientId.startsWith("invalid-")) missing.add("GOOGLE_CLIENT_ID");
        if (clientSecret.isBlank() || clientSecret.startsWith("invalid-")) missing.add("GOOGLE_CLIENT_SECRET");
        if (!missing.isEmpty()) throw new IllegalStateException("Variáveis Google ausentes: " + String.join(", ", missing));
        ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId).clientSecret(clientSecret)
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
