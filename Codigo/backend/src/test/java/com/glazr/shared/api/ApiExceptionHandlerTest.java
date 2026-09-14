package com.glazr.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

class ApiExceptionHandlerTest {

    @Test
    void unknownPublicPathUsesNotFoundInsteadOfInternalError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        ApiExceptionHandler handler = new ApiExceptionHandler(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        var problem = handler.resourceNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/oauth2/authorization/google"), request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getProperties()).containsEntry("code", "RESOURCE_NOT_FOUND");
    }

    @ParameterizedTest
    @CsvSource({
            "uk_users_email_ci,DUPLICATE_EMAIL",
            "uk_access_profiles_name_ci,DUPLICATE_ACCESS_PROFILE",
            "uk_kits_name_ci,DUPLICATE_KIT",
            "fk_user_access_profiles_profile,ACCESS_PROFILE_IN_USE",
            "some_other_constraint,DATA_CONFLICT"
    })
    void translatesKnownDatabaseConstraintsToStableCodes(String constraint, String expectedCode) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(request.getMethod()).thenReturn("DELETE");
        ConstraintViolationException databaseCause = new ConstraintViolationException(
                "constraint", new SQLException("duplicate"), constraint);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("conflict", databaseCause);
        ApiExceptionHandler handler = new ApiExceptionHandler(
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        var problem = handler.dataConflict(exception, request);

        assertThat(problem.getProperties()).containsEntry("code", expectedCode);
    }
}
