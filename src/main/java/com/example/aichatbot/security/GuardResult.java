package com.example.aichatbot.security;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
/**
 * GuardResult is a class that is used to return the result of a guard check.
 * It contains a boolean indicating whether the check was successful or not,
 * a reason for the failure, and a list of violations.
 */
@Data
@AllArgsConstructor
public class GuardResult {
    private boolean blocked;
    private String reason;
    private List<String> violations;

    public static GuardResult safe() {
        return new GuardResult(false, null, List.of());
    }

    public static GuardResult blocked(String reason, List<String> violations) {
        return new GuardResult(true, reason, violations);
    }
}