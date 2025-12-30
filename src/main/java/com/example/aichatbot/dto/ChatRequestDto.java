package com.example.aichatbot.dto;

/**
 * Request DTO for chat messages
 */
public record ChatRequestDto(
                Long conversationId,
                String message,
                BotConfigDto botConfig) {
}
