package ru.yandex.practicum.exception;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestControllerAdvice("ru.yandex.practicum")
public class ErrorHandler {
    private final Environment environment;

    public ErrorHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        log.warn("Validation error in request: {}", e.getMessage(), e);

        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .toList();

        return new ErrorResponse(
                "Validation failed",
                errors.isEmpty() ? e.getMessage() : String.join("; ", errors)
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRequestHeaderException(final MissingRequestHeaderException e) {
        log.warn("Missing request header: {}", e.getMessage(), e);
        return new ErrorResponse("Missing required header", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(final NotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage(), e);
        return new ErrorResponse("Resource not found", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(final ValidationException e) {
        log.warn("Business validation error: {}", e.getMessage(), e);
        return new ErrorResponse("Validation failed", e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(final BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage(), e);
        return new ErrorResponse("Bad request", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        log.warn("Invalid request parameter: {}", e.getMessage(), e);
        return new ErrorResponse(
                "Bad request",
                String.format("Parameter '%s' has invalid value '%s'", e.getName(), e.getValue()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(final ResponseStatusException e) {
        log.warn("ResponseStatusException: status={}, reason={}", e.getStatusCode(), e.getReason(), e);

        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(
                        e.getStatusCode().toString(),
                        e.getReason() != null ? e.getReason() : e.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleIllegalStateException(final IllegalStateException e) {
        log.error("IllegalStateException occurred in application code", e);

        boolean isDevelopment = environment.acceptsProfiles(Profiles.of("dev", "test"));
        StackTraceElement[] stackTrace = isDevelopment ? e.getStackTrace() : null;

        return new ErrorResponse(
                "Internal server error",
                e.getMessage(),
                stackTrace
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(final RuntimeException e) {
        boolean isDevelopment = environment.acceptsProfiles(Profiles.of("dev", "test"));

        if (isDevelopment) {
            log.warn("Development environment error", e);
            return new ErrorResponse(
                    "Internal server error",
                    e.getMessage() != null ? e.getMessage() : "No message"
            );
        }

        log.error("Internal server error", e);
        return new ErrorResponse("Internal server error", "An unexpected error occurred");
    }
}