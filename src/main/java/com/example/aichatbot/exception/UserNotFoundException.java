package com.example.aichatbot.exception;

/**
 * Exception thrown when a requested user is not found.
 * Results in HTTP 404 NOT FOUND response.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
