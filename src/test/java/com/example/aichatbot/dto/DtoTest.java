package com.example.aichatbot.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoTest {

    @Test
    void loginRequest_ConstructorAndAccessors() {
        // Act
        LoginRequestDto request = new LoginRequestDto("testuser", "password123");

        // Assert
        assertEquals("testuser", request.username());
        assertEquals("password123", request.password());
    }

    @Test
    void chatRequest_ConstructorAndAccessors() {
        // Arrange
        BotConfigDto config = new BotConfigDto("friendly");

        // Act
        ChatRequestDto request = new ChatRequestDto(100L, "Hello AI", config);

        // Assert
        assertEquals(100L, request.conversationId());
        assertEquals("Hello AI", request.message());
        assertNotNull(request.botConfig());
        assertEquals("friendly", request.botConfig().personality());
    }

    @Test
    void chatResponse_ConstructorAndAccessors() {
        // Act
        ChatResponseDto response = new ChatResponseDto("AI response text");

        // Assert
        assertEquals("AI response text", response.response());
    }

    @Test
    void botConfig_ConstructorAndAccessors() {
        // Act
        BotConfigDto config = new BotConfigDto("professional");

        // Assert
        assertEquals("professional", config.personality());
    }

    @Test
    void statisticsDTO_Constructor() {
        // Act
        StatisticsDto stats = new StatisticsDto(100, 250, 1500, 50, 15, 10000);

        // Assert
        assertEquals(100, stats.totalUsers());
        assertEquals(250, stats.totalConversations());
        assertEquals(1500, stats.totalMessages());
        assertEquals(50, stats.totalDocuments());
        assertEquals(15, stats.activeConversations24h());
        assertEquals(10000, stats.totalTokens());
    }

    @Test
    void loginRequest_Equals() {
        // Arrange
        LoginRequestDto request1 = new LoginRequestDto("user", "pass");
        LoginRequestDto request2 = new LoginRequestDto("user", "pass");

        // Assert
        assertEquals(request1, request2);
    }

    @Test
    void chatResponse_ToString() {
        // Arrange
        ChatResponseDto response = new ChatResponseDto("Test");

        // Assert
        String toString = response.toString();
        assertTrue(toString.contains("Test"));
    }
}
