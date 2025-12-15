package com.example.aichatbot.dto;

import lombok.Data;

/**
 * Request DTO for chat messages
 */
@Data
public class ChatRequest {
    private Integer userId;
    private Integer conversationId;
    private String message;
    private BotConfigDto botConfig;
}
