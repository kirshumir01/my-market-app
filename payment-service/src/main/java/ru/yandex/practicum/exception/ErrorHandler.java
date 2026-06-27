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

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "ru.yandex.practicum")
public class ErrorHandler {

    private final Environment environment;

    public ErrorHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleWebExchangeBindException(WebExchangeBindException ex) {
        String description = ex.getFieldErrors().stream()
                .map(error -> "%s: %s".formatted(
                        error.getField(),
                        error.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        if (description.isBlank()) {
            description = ex.getMessage();
        }

        log.warn("Validation failed: {}", description);

        return new ErrorResponse(
                "Validation failed",
                description
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        return new ErrorResponse(
                "Invalid argument",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleServerWebInputException(ServerWebInputException ex) {
        String description = ex.getReason() != null
                ? ex.getReason()
                : ex.getMessage();

        log.warn("Bad request: {}", description);

        return new ErrorResponse(
                "Bad request",
                description
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException ex) {
        boolean isDevelopment = environment.acceptsProfiles(Profiles.of("dev", "test"));

        if (isDevelopment) {
            log.error("Unexpected error", ex);

            return new ErrorResponse(
                    "Internal server error",
                    Objects.requireNonNullElse(ex.getMessage(), "No message")
            );
        }

        log.error("Unexpected error", ex);

        return new ErrorResponse(
                "Internal server error",
                "An unexpected error occurred"
        );
    }
}