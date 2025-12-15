package com.example.aichatbot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmGuardServiceTest {

    private LlmGuardService llmGuardService;

    @BeforeEach
    void setUp() {
        llmGuardService = new LlmGuardService();
        // Set the max input length via reflection
        ReflectionTestUtils.setField(llmGuardService, "maxInputLength", 1000);
    }

    @Test
    void validateInput_WhenInputIsNull_ShouldReturnBlocked() {
        // Act
        GuardResult result = llmGuardService.validateInput(null);

        // Assert
        assertTrue(result.isBlocked());
        assertEquals("Empty input", result.getReason());
        assertTrue(result.getViolations().contains("Input cannot be empty"));
    }

    @Test
    void validateInput_WhenInputIsEmpty_ShouldReturnBlocked() {
        // Act
        GuardResult result = llmGuardService.validateInput("   ");

        // Assert
        assertTrue(result.isBlocked());
        assertEquals("Empty input", result.getReason());
    }

    @Test
    void validateInput_WhenInputExceedsMaxLength_ShouldReturnBlocked() {
        // Arrange
        String longInput = "a".repeat(1001);

        // Act
        GuardResult result = llmGuardService.validateInput(longInput);

        // Assert
        assertTrue(result.isBlocked());
        assertEquals("Input too long", result.getReason());
        assertTrue(result.getViolations().get(0).contains("1000"));
    }

    @Test
    void validateInput_WhenInputContainsHarmfulPattern_ShouldReturnBlocked() {
        // Act
        GuardResult result = llmGuardService.validateInput("This is a <script>alert('xss')</script> test");

        // Assert
        assertTrue(result.isBlocked());
        assertEquals("Input validation failed", result.getReason());
        assertFalse(result.getViolations().isEmpty());
    }

    @Test
    void validateInput_WhenInputIsValid_ShouldReturnSafe() {
        // Act
        GuardResult result = llmGuardService.validateInput("This is a safe input");

        // Assert
        assertFalse(result.isBlocked());
        assertNull(result.getReason());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void validateOutput_WhenOutputIsNull_ShouldReturnBlocked() {
        // Act
        GuardResult result = llmGuardService.validateOutput(null);

        // Assert
        assertTrue(result.isBlocked());
        assertEquals("Empty output", result.getReason());
        assertTrue(result.getViolations().contains("Output cannot be empty"));
    }

    @Test
    void validateOutput_WhenOutputContainsSensitiveData_ShouldReturnBlocked() {
        // Act
        GuardResult result = llmGuardService.validateOutput("Your password=secret123");

        // Assert
        assertTrue(result.isBlocked());
        assertEquals("Output validation failed", result.getReason());
        assertFalse(result.getViolations().isEmpty());
    }

    @Test
    void validateOutput_WhenOutputIsValid_ShouldReturnSafe() {
        // Act
        GuardResult result = llmGuardService.validateOutput("This is a safe output");

        // Assert
        assertFalse(result.isBlocked());
        assertNull(result.getReason());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void sanitizeOutput_WhenInputIsNull_ShouldReturnEmptyString() {
        // Act
        String result = llmGuardService.sanitizeOutput(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    void sanitizeOutput_WhenInputContainsHtml_ShouldEscapeHtml() {
        // Act
        String result = llmGuardService.sanitizeOutput("<script>alert('xss')</script>");

        // Assert
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;", result);
    }

    @Test
    void sanitizeOutput_WhenInputIsSafe_ShouldReturnSameString() {
        // Arrange
        String safeInput = "This is a safe string with numbers 123";

        // Act
        String result = llmGuardService.sanitizeOutput(safeInput);

        // Assert
        assertEquals(safeInput, result);
    }
}