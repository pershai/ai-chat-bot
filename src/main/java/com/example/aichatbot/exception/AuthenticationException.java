package com.example.aichatbot.exception;

/**
 * Exception thrown when authentication is missing or invalid.
 * Results in HTTP 401 UNAUTHORIZED response.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
