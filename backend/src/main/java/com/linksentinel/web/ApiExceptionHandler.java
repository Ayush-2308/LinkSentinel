package com.linksentinel.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("validation_failed", "Check the highlighted fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable() {
        return ResponseEntity.badRequest().body(new ApiError("bad_request", "Request body is not valid JSON", Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException ex) {
        String error = switch (ex.getStatusCode().value()) {
            case 400 -> "bad_request";
            case 401 -> "unauthorized";
            case 404 -> "not_found";
            case 409 -> "conflict";
            case 410 -> "gone";
            default -> "error";
        };
        String message = ex.getReason() == null ? error : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(error, message, Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict() {
        return ResponseEntity.status(409).body(new ApiError("conflict", "That value is already in use", Map.of()));
    }
}
