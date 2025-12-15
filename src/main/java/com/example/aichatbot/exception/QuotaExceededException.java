package com.example.aichatbot.exception;

import lombok.Getter;
/**
 * Exception thrown when a user exceeds their quota for a specific resource.
 * Results in HTTP 429 TOO MANY REQUESTS response.
 */
@Getter
public class QuotaExceededException extends RuntimeException {
    private final String retryAfter;

    public QuotaExceededException(String message, String retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

}
