package com.glazr.shared.api;

import com.glazr.authentication.application.AuthenticationFailedException;
import com.glazr.shared.exception.ApplicationException;
import com.glazr.user.application.LastActiveAdministratorException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dados inválidos", request);
        List<Map<String, String>> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();
        problem.setProperty("fieldErrors", fields);
        return problem;
    }

    @ExceptionHandler(ApplicationException.class)
    ProblemDetail application(ApplicationException exception, HttpServletRequest request) {
        return problem(exception.status(), exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail authentication(AuthenticationFailedException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso negado", request);
    }

    @ExceptionHandler(LastActiveAdministratorException.class)
    ProblemDetail lastAdministrator(LastActiveAdministratorException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "LAST_ACTIVE_ADMINISTRATOR", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail illegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformedBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Corpo da solicitação inválido", request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ProblemDetail invalidParameter(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_PARAMETER", "Parâmetro da solicitação inválido", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        String constraint = constraintName(exception);
        if (constraint.contains("uk_users_email_ci")) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Já existe um usuário com este e-mail", request);
        }
        if (constraint.contains("uk_access_profiles_name_ci")) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_ACCESS_PROFILE", "Já existe um perfil com este nome", request);
        }
        if (constraint.contains("uk_kits_name_ci")) {
            return problem(HttpStatus.CONFLICT, "DUPLICATE_KIT", "Já existe um kit com este nome", request);
        }
        if (constraint.contains("fk_user_access_profiles_profile")
                && "DELETE".equalsIgnoreCase(request.getMethod())) {
            return problem(HttpStatus.CONFLICT, "ACCESS_PROFILE_IN_USE", "O perfil está associado a usuários", request);
        }
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "Os dados conflitam com um registro existente", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Erro interno ao processar {} {}", request.getMethod(), request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Não foi possível processar a solicitação", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail resourceNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso não encontrado", request);
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", clock.instant());
        return problem;
    }

    private Map<String, String> fieldError(FieldError error) {
        return Map.of("field", error.getField(), "message",
                error.getDefaultMessage() == null ? "Valor inválido" : error.getDefaultMessage());
    }

    private String constraintName(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                return violation.getConstraintName().toLowerCase(java.util.Locale.ROOT);
            }
            current = current.getCause();
        }
        return "";
    }
}
