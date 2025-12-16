package com.example.aichatbot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler using RFC 7807 ProblemDetail standard.
 * Complies with Architecture Rule #5: Use ProblemDetail for error handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://api.aichatbot.example.com/errors/";

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problem.setTitle("Invalid Argument");
        problem.setType(URI.create(ERROR_TYPE_BASE + "invalid-argument"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage());
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(ERROR_TYPE_BASE + "internal-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed: " + violations);
        problem.setTitle("Validation Error");
        problem.setType(URI.create(ERROR_TYPE_BASE + "validation-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        problem.setProperty("violations", ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList()));
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create(ERROR_TYPE_BASE + "not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage());
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create(ERROR_TYPE_BASE + "authentication-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ProblemDetail handleQuotaExceededException(QuotaExceededException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage());
        problem.setTitle("API Quota Exceeded");
        problem.setType(URI.create(ERROR_TYPE_BASE + "quota-exceeded"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        if (ex.getRetryAfter() != null) {
            problem.setProperty("retryAfter", ex.getRetryAfter());
        }
        return problem;
    }

    @ExceptionHandler(InfrastructureException.class)
    public ProblemDetail handleInfrastructureException(InfrastructureException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage());
        problem.setTitle("Service Unavailable");
        problem.setType(URI.create(ERROR_TYPE_BASE + "infrastructure-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(ERROR_TYPE_BASE + "internal-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        problem.setProperty("message", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DocumentUpdateException.class)
    public ProblemDetail handleDocumentUpdateException(DocumentUpdateException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problem.setTitle("Document Update Failed");
        problem.setType(URI.create(ERROR_TYPE_BASE + "document-update-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create(ERROR_TYPE_BASE + "bad-request"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", getPath(request));
        return problem;
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
