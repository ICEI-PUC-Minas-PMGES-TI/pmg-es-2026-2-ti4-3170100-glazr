package com.glazr.google.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DiscardingOAuth2AuthorizedClientRepositoryTest {
    @Test
    void neverStoresOrReturnsProviderTokens() {
        var repository = new DiscardingOAuth2AuthorizedClientRepository();
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        Authentication principal = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        repository.saveAuthorizedClient(client, principal, request, response);

        assertThat(repository.<OAuth2AuthorizedClient>loadAuthorizedClient("google", principal, request)).isNull();
        repository.removeAuthorizedClient("google", principal, request, response);
    }
}
