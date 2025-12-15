package com.example.aichatbot.exception;

/**
 * Exception thrown when infrastructure components fail to initialize or
 * operate.
 * Results in HTTP 503 SERVICE UNAVAILABLE response.
 */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String component, String message) {
        super(String.format("Infrastructure failure in %s: %s", component, message));
    }

    public InfrastructureException(String component, String message, Throwable cause) {
        super(String.format("Infrastructure failure in %s: %s", component, message), cause);
    }
}
