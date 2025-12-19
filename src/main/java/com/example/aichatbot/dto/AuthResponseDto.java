package com.example.aichatbot.dto;

import java.util.Set;

/**
 * Response DTO for authentication endpoints
 */
public record AuthResponseDto(String token, String refreshToken, String userId, String username,
        Set<String> roles) {
}
