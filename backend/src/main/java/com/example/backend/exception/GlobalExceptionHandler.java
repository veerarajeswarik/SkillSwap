package com.example.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catches exceptions thrown by ANY controller (or the services it calls)
 * and turns them into one consistent JSON error format:
 *
 * {
 *   "timestamp": "2026-09-28T15:40:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Member not found with id 99",
 *   "path": "/api/members/99"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404: member / skill offer / session id does not exist.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 400: requester does not have enough credits.
    @ExceptionHandler(InsufficientCreditException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientCredit(InsufficientCreditException ex,
                                                                        HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 409: a business rule forbids this in the current state
    // (duplicate email, own skill, session already confirmed, too many hours...).
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // 400: @Valid failed on a DTO. Adds a per-field "errors" map for the frontend.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ResponseEntity<Map<String, Object>> response =
                build(HttpStatus.BAD_REQUEST, "Validation failed", request);
        response.getBody().put("errors", fieldErrors);
        return response;
    }

    // 400: the body is not valid JSON, or a value has the wrong type ("hours": "abc").
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request);
    }

    // 400: a URL value has the wrong type (/api/members/abc, ?status=BANANA).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                  HttpServletRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // 409: the database rejected the data, e.g. two registrations with the same email
    // at the same moment both passed existsByEmail, and the UNIQUE constraint caught the second.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                   HttpServletRequest request) {
        log.warn("Database constraint violation on {}", request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "The data conflicts with an existing record", request);
    }

    // Everything else.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleEverythingElse(Exception ex,
                                                                    HttpServletRequest request) {
        // Spring's own web exceptions (unknown URL -> 404, wrong HTTP method -> 405, ...)
        // already know their correct status; keep it instead of turning them into 500.
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return build(status, ex.getMessage(), request);
        }

        // A real bug. Log the full stack trace for developers,
        // but never send internal details to the client.
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatusCode status, String message,
                                                      HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        HttpStatus known = HttpStatus.resolve(status.value());
        body.put("error", known != null ? known.getReasonPhrase() : "Error");
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
