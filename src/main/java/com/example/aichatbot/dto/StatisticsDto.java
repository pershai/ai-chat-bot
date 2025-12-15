package com.example.aichatbot.dto;

/**
 * Statistics DTO for system metrics
 */
public record StatisticsDto(
                long totalUsers,
                long totalConversations,
                long totalMessages,
                long totalDocuments,
                long activeConversations24h,
                long totalTokens) {
}
