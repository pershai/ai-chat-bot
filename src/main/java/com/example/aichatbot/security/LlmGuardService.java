package com.example.aichatbot.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LlmGuardService {

    @Value("${llm.guard.max-input-length:1000}")
    private int maxInputLength;

    private final List<Pattern> inputValidationPatterns = List.of(
            // SQL Injection - Only actual SQL commands, not individual symbols
            Pattern.compile("(?i)\\b(SELECT|INSERT|UPDATE|DELETE)\\s+.+\\s+FROM\\b"),
            Pattern.compile("(?i)\\b(DROP|ALTER|CREATE)\\s+(TABLE|DATABASE)\\b"),
            Pattern.compile("(?i)\\bUNION\\s+SELECT\\b"),
            // XSS
            Pattern.compile("(?i)(<script[^>]*>.*?</script>|javascript:|<.*?on\\w+\\s*=)"),
            // Sensitive data - only when actually revealing credentials
            Pattern.compile("(?i)(password|secret|api[_-]?key|token)\\s*[:=]\\s*['\"][^'\"]{10,}['\"]"),
            // Path traversal
            Pattern.compile("(\\.\\./|\\.\\\\){2,}"));

    private final List<Pattern> outputValidationPatterns = List.of(
            // Sensitive data patterns
            Pattern.compile("(?i)(password|secret|api[_-]?key|token|auth|credential)[\\s:=]+([^\\s]+)"),
            // Harmful content patterns
            Pattern.compile("(?i)(hack|exploit|vulnerability|malware|virus|trojan|ransomware)"));

    public GuardResult validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return GuardResult.blocked("Empty input", List.of("Input cannot be empty"));
        }

        if (input.length() > maxInputLength) {
            return GuardResult.blocked("Input too long",
                    List.of("Input exceeds maximum length of " + maxInputLength + " characters"));
        }

        List<String> violations = new ArrayList<>();

        for (Pattern pattern : inputValidationPatterns) {
            if (pattern.matcher(input).find()) {
                violations.add("Input contains potentially harmful pattern: " + pattern.pattern());
            }
        }

        return violations.isEmpty() ? GuardResult.safe() : GuardResult.blocked("Input validation failed", violations);
    }

    public GuardResult validateOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return GuardResult.blocked("Empty output", List.of("Output cannot be empty"));
        }

        List<String> violations = new ArrayList<>();

        for (Pattern pattern : outputValidationPatterns) {
            if (pattern.matcher(output).find()) {
                violations.add("Output contains potentially harmful pattern: " + pattern.pattern());
            }
        }

        return violations.isEmpty() ? GuardResult.safe() : GuardResult.blocked("Output validation failed", violations);
    }

    public String sanitizeOutput(String output) {
        if (output == null)
            return "";
        return output.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}