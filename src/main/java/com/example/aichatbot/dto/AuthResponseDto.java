package com.example.aichatbot.dto;

/**
 * Response DTO for authentication endpoints
 */
public record AuthResponseDto(String token, Integer userId, String username) {
}
