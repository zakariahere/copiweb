package com.elzakaria.copiweb.web;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
        log.debug("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                fe -> fe.getField(),
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                (a, b) -> a
            ));
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Validation failed", "fields", fieldErrors));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        log.debug("Async request timed out: {}", ex.getClass().getSimpleName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex,
            jakarta.servlet.http.HttpServletResponse response) {
        if (response.isCommitted()) {
            // SSE or streaming response already started — don't try to write again
            if (isExpectedClientDisconnect(ex)) {
                log.debug("Client disconnected from streaming response: {}", ex.getMessage());
            } else {
                log.warn("Exception after response committed: {}", ex.getMessage());
            }
            return null;
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Internal server error"));
    }

    static boolean isExpectedClientDisconnect(Throwable throwable) {
        var current = throwable;
        while (current != null) {
            var simpleName = current.getClass().getSimpleName();
            if ("AsyncRequestNotUsableException".equals(simpleName)
                    || "ClientAbortException".equals(simpleName)
                    || "EOFException".equals(simpleName)) {
                return true;
            }

            var message = current.getMessage();
            if (message != null) {
                var normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("disconnected client")
                        || normalized.contains("broken pipe")
                        || normalized.contains("connection reset by peer")
                        || normalized.contains("connection aborted")
                        || normalized.contains("an established connection was aborted")
                        || normalized.contains("forcibly closed by the remote host")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
