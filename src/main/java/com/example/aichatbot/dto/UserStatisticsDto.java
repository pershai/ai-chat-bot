package com.example.aichatbot.dto;

/**
 * User-specific statistics DTO
 */
public record UserStatisticsDto(
        long myConversations,
        long myMessages,
        long totalMessages,
        long totalDocuments,
        long myActiveConversations24h,
        long myTotalTokens) {
}
