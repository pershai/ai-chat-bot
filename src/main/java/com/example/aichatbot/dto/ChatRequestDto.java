package com.example.aichatbot.dto;

/**
 * Request DTO for chat messages
 */
public record ChatRequestDto(
        Integer userId,
        Integer conversationId,
        String message,
        BotConfigDto botConfig) {
}
