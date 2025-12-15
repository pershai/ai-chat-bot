package com.example.aichatbot.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for simplified exception classes.
 * These exceptions now extend RuntimeException and don't carry HTTP status or
 * error codes.
 */
class ExceptionTest {

    @Test
    void authenticationException_CreatesWithMessage() {
        // Act
        AuthenticationException exception = new AuthenticationException("No authentication found");

        // Assert
        assertEquals("No authentication found", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void resourceNotFoundException_CreatesWithTypeAndId() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "john.doe");

        // Assert
        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("john.doe"));
        assertEquals("User not found with id: john.doe", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void resourceNotFoundException_CreatesWithMessage() {
        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException("Custom message");

        // Assert
        assertEquals("Custom message", exception.getMessage());
    }

    @Test
    void infrastructureException_CreatesWithComponentAndMessage() {
        // Act
        InfrastructureException exception = new InfrastructureException("Qdrant", "Failed to connect");

        // Assert
        assertTrue(exception.getMessage().contains("Qdrant"));
        assertTrue(exception.getMessage().contains("Failed to connect"));
        assertEquals("Infrastructure failure in Qdrant: Failed to connect", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void infrastructureException_CreatesWithComponentMessageAndCause() {
        // Arrange
        Throwable cause = new RuntimeException("Connection timeout");

        // Act
        InfrastructureException exception = new InfrastructureException("Qdrant", "Failed to connect", cause);

        // Assert
        assertTrue(exception.getMessage().contains("Qdrant"));
        assertTrue(exception.getMessage().contains("Failed to connect"));
        assertEquals(cause, exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }
}
