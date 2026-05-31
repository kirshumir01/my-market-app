package ru.yandex.practicum.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    String error;
    String description;
    StackTraceElement[] stackTrace;

    public ErrorResponse(String error, String description) {
        this.error = error;
        this.description = description;
    }

    public ErrorResponse(String error, String description, StackTraceElement[] stackTrace) {
        this.error = error;
        this.description = description;
        this.stackTrace = stackTrace;
    }
}