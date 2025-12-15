package com.example.aichatbot.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 NOT FOUND response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
