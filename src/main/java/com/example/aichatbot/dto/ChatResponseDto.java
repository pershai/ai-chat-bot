package com.example.aichatbot.dto;

/**
 * Response DTO for chat messages and general API responses
 */
public record ChatResponseDto(String response, Long conversationId) {
    public ChatResponseDto(String response) {
        this(response, null);
    }
}
