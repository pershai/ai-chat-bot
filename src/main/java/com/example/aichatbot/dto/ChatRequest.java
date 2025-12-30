package com.example.aichatbot.dto;

import lombok.Data;

/**
 * Request DTO for chat messages
 */
@Data
public class ChatRequest {
    private String userId;
    private Long conversationId;
    private String message;
    private BotConfigDto botConfig;
}
