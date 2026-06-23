package com.pridepin.pridepin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Central exception handler: maps domain and framework exceptions to HTTP status codes and JSON error bodies.
 * Validation errors return field-level messages; security and generic errors return a single message.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain exceptions ──────────────────────────────────────────────────

    /** 404 when a requested resource (user, location, review) is not found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** 409 when username/email is already taken or user already reviewed the location. */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** 403 when the user is not allowed to perform the action (e.g. not owner/admin). */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ── Spring Security exceptions ─────────────────────────────────────────

    /** 403 when Spring Security denies access (e.g. missing ADMIN role). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Access denied");
    }

    /** 400 for bad request (e.g. invalid or expired verification token). */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 403 when login is attempted with an unverified account (email verification enabled). */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return buildError(HttpStatus.FORBIDDEN,
                "Please verify your email address before logging in. " +
                "Check your inbox or use the resend link.");
    }

    /** 401 for wrong username/password or other authentication failure. */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(Exception ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    // ── Validation exceptions ──────────────────────────────────────────────

    /** 400 with a map of field names to validation messages when @Valid fails on request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                fieldErrors,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    // ── Catch-all ──────────────────────────────────────────────────────────

    /** 500 for any unhandled exception; message is generic, full stack trace is logged. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ── Response types (records) ───────────────────────────────────────────

    /** Builds a JSON error response with status code, message, and timestamp. */
    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, LocalDateTime.now()));
    }

    /** JSON shape for most API errors: status code, message, timestamp. */
    public record ErrorResponse(int status, String message, LocalDateTime timestamp) {}

    /** JSON shape for validation errors: status, message, and a map of field name → error message. */
    public record ValidationErrorResponse(
            int status,
            String message,
            Map<String, String> errors,
            LocalDateTime timestamp
    ) {}
}
