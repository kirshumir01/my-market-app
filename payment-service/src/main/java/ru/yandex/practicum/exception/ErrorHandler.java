package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;

@Slf4j
@RestControllerAdvice(basePackages = "ru.yandex.practicum")
public class ErrorHandler {

    private final Environment environment;

    public ErrorHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleWebExchangeBindException(WebExchangeBindException e) {
        log.warn("Validation error in request: {}", e.getMessage(), e);

        List<String> errors = e.getFieldErrors()
                .stream()
                .map(error ->
                        "%s: %s".formatted(
                                error.getField(),
                                error.getDefaultMessage()))
                .toList();

        return new ErrorResponse(
                "Validation failed",
                errors.isEmpty() ? e.getMessage(): String.join("; ", errors)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());

        return new ErrorResponse(
                "Invalid argument",
                e.getMessage()
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleServerWebInputException(ServerWebInputException e) {
        log.warn("Invalid request parameter: {}", e.getMessage(), e);

        return new ErrorResponse(
                "Bad request",
                e.getReason() != null ? e.getReason() : e.getMessage()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException e) {
        boolean isDevelopment = environment.acceptsProfiles(Profiles.of("dev", "test"));

        if (isDevelopment) {
            log.warn("Development environment error", e);

            return new ErrorResponse(
                    "Internal server error",
                    e.getMessage() != null ? e.getMessage() : "No message"
            );
        }

        log.error("Internal server error", e);

        return new ErrorResponse(
                "Internal server error",
                "An unexpected error occurred"
        );
    }
}