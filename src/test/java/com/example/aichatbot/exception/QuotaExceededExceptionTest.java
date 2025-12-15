package com.example.aichatbot.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuotaExceededExceptionTest {

    @Test
    void constructor_WhenCalled_SetsPropertiesCorrectly() {
        // Arrange
        String message = "Limit reached";
        String retryAfter = "60s";

        // Act
        QuotaExceededException exception = new QuotaExceededException(message, retryAfter);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(retryAfter, exception.getRetryAfter());
    }

    @Test
    void inheritance_IsRuntimeException() {
        // Act
        QuotaExceededException exception = new QuotaExceededException("msg", "10s");

        // Assert
        assertTrue(exception instanceof RuntimeException);
    }
}
