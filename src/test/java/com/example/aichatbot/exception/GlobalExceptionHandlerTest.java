package com.example.aichatbot.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @Test
    void handleIllegalArgumentException_ReturnsBadRequest() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input provided");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // Act
        ProblemDetail problem = exceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Assert
        assertEquals(400, problem.getStatus());
        assertEquals("Invalid Argument", problem.getTitle());
        assertEquals("Invalid input provided", problem.getDetail());
        assertNotNull(problem.getType());
        assertTrue(problem.getType().toString().contains("invalid-argument"));
        assertEquals("/api/test", problem.getProperties().get("path"));
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void handleIllegalStateException_ReturnsInternalServerError() {
        // Arrange
        IllegalStateException exception = new IllegalStateException("System in invalid state");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // Act
        ProblemDetail problem = exceptionHandler.handleIllegalStateException(exception, webRequest);

        // Assert
        assertEquals(500, problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("System in invalid state", problem.getDetail());
        assertTrue(problem.getType().toString().contains("internal-error"));
    }

    @Test
    void handleMethodArgumentNotValidException_ReturnsBadRequest() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("user", "email", "must not be blank");
        FieldError error2 = new FieldError("user", "password", "must be at least 8 characters");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/users");

        // Act
        ProblemDetail problem = exceptionHandler.handleValidationException(exception, webRequest);

        // Assert
        assertEquals(400, problem.getStatus());
        assertEquals("Validation Error", problem.getTitle());
        assertTrue(problem.getDetail().contains("email"));
        assertTrue(problem.getDetail().contains("password"));
        assertTrue(problem.getType().toString().contains("validation-error"));

        @SuppressWarnings("unchecked")
        List<String> violations = (List<String>) problem.getProperties().get("violations");
        assertEquals(2, violations.size());
    }

    @Test
    void handleResourceNotFoundException_ReturnsNotFound() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "john.doe");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/users/john.doe");

        // Act
        ProblemDetail problem = exceptionHandler.handleResourceNotFoundException(exception, webRequest);

        // Assert
        assertEquals(404, problem.getStatus());
        assertEquals("Resource Not Found", problem.getTitle());
        assertTrue(problem.getDetail().contains("User"));
        assertTrue(problem.getDetail().contains("john.doe"));
        assertTrue(problem.getType().toString().contains("not-found"));
    }

    @Test
    void handleAuthenticationException_ReturnsUnauthorized() {
        // Arrange
        AuthenticationException exception = new AuthenticationException("Invalid credentials");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/auth/login");

        // Act
        ProblemDetail problem = exceptionHandler.handleAuthenticationException(exception, webRequest);

        // Assert
        assertEquals(401, problem.getStatus());
        assertEquals("Authentication Failed", problem.getTitle());
        assertEquals("Invalid credentials", problem.getDetail());
        assertTrue(problem.getType().toString().contains("authentication-error"));
    }

    @Test
    void handleInfrastructureException_ReturnsServiceUnavailable() {
        // Arrange
        InfrastructureException exception = new InfrastructureException("Database", "Connection failed",
                new RuntimeException("Connection timeout"));
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // Act
        ProblemDetail problem = exceptionHandler.handleInfrastructureException(exception, webRequest);

        // Assert
        assertEquals(503, problem.getStatus());
        assertEquals("Service Unavailable", problem.getTitle());
        assertTrue(problem.getDetail().contains("Database"));
        assertTrue(problem.getDetail().contains("Connection failed"));
        assertTrue(problem.getType().toString().contains("infrastructure-error"));
    }

    @Test
    void handleQuotaExceededException_ReturnsTooManyRequests() {
        // Arrange
        QuotaExceededException exception = new QuotaExceededException("Rate limit 10/min exceeded", "60s");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/ai/chat");

        // Act
        ProblemDetail problem = exceptionHandler.handleQuotaExceededException(exception, webRequest);

        // Assert
        assertEquals(429, problem.getStatus());
        assertEquals("API Quota Exceeded", problem.getTitle());
        assertEquals("Rate limit 10/min exceeded", problem.getDetail());
        assertEquals("60s", problem.getProperties().get("retryAfter"));
        assertTrue(problem.getType().toString().contains("quota-exceeded"));
    }

    @Test
    void handleAllExceptions_ReturnsInternalServerError() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // Act
        ProblemDetail problem = exceptionHandler.handleAllExceptions(exception, webRequest);

        // Assert
        assertEquals(500, problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("An unexpected error occurred", problem.getDetail());
        assertEquals("Unexpected error", problem.getProperties().get("message"));
        assertTrue(problem.getType().toString().contains("internal-error"));
    }
}
