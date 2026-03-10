package com.proxy.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProxyValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ProxyValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody(400, ex.getMessage()));
    }

    @ExceptionHandler(OriginNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleOriginNotAllowed(OriginNotAllowedException ex) {
        log.warn("Origin blocked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(403, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled error: {}", ex.getMessage());
        // Never expose internal details to clients
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "An internal error occurred. Please try again."));
    }

    private Map<String, Object> errorBody(int status, String message) {
        return Map.of(
                "status", status,
                "error", message,
                "timestamp", Instant.now().toString()
        );
    }
}
