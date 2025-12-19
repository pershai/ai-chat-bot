package com.example.aichatbot.service;

import com.example.aichatbot.dto.BotConfigDto;
import com.example.aichatbot.exception.QuotaExceededException;
import com.example.aichatbot.security.LlmGuardService;
import com.example.aichatbot.security.ValidateInput;
import com.example.aichatbot.security.ValidateOutput;
import com.example.aichatbot.service.graph.RagState;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final CompiledGraph<RagState> ragGraphRunner;
    private final ConversationService conversationService;
    @SuppressWarnings("unused")
    private final LlmGuardService guardService;

    @ValidateInput
    @ValidateOutput
    @CircuitBreaker(name = "gemini", fallbackMethod = "processChatFallback")
    public String processChat(String userId, Long conversationId, String message, BotConfigDto botConfig) {
        try {
            // LangGraph State Setup
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("query", message);
            inputs.put("conversationId", String.valueOf(conversationId));
            inputs.put("userId", userId);

            Optional<RagState> result = ragGraphRunner.invoke(inputs);

            // Extract Response
            RagState finalState = result.orElseThrow(() -> new IllegalStateException("Graph returned empty state"));

            String response = (String) finalState.data().get("response");
            if (response == null) {
                response = "I encountered an error processing your request.";
            }

            conversationService.addMessage(conversationId, "user", message);

            Map<String, Integer> tokenUsage = finalState.getTokenUsage();
            int inputTokens = tokenUsage.getOrDefault("totalInputTokens", 0);
            int outputTokens = tokenUsage.getOrDefault("totalOutputTokens", 0);

            conversationService.addMessage(conversationId, "assistant", response, inputTokens, outputTokens);

            return response;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing chat for user {}: {}", userId, e.getMessage(), e);

            // Check if this is a Gemini API quota exceeded error
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("RESOURCE_EXHAUSTED")) {
                String retryAfter = extractRetryTime(errorMessage);
                String userMessage = "API quota exceeded. Please wait a moment and try again.";
                throw new QuotaExceededException(userMessage, retryAfter);
            }

            throw new IllegalStateException("Failed to process chat message: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    public String processChatFallback(String userId, Long conversationId, String message, BotConfigDto botConfig,
            Throwable t) {
        log.error("Circuit breaker open or exception fallback for user {}: {}", userId, t.getMessage());
        return "The AI service is currently unavailable. Please try again later.";
    }

    private String extractRetryTime(String errorMessage) {
        try {
            // Match patterns like "retry in 26.467792637s" or "retry in X seconds"
            Pattern pattern = Pattern.compile("retry in ([0-9.]+)s");
            Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                double seconds = Double.parseDouble(matcher.group(1));
                int roundedSeconds = (int) Math.ceil(seconds);
                if (roundedSeconds < 60) {
                    return roundedSeconds + " seconds";
                } else {
                    int minutes = roundedSeconds / 60;
                    return minutes + " minute" + (minutes > 1 ? "s" : "");
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to extract retry time from error: {}", errorMessage);
        }
        return "a few moments";
    }

}
