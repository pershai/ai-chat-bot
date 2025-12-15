package com.example.aichatbot.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LlmGuardAspect {

    private final LlmGuardService guardService;

    @Around("@annotation(ValidateInput)")
    public Object validateInput(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof String) {
                GuardResult result = guardService.validateInput((String) arg);
                if (result.isBlocked()) {
                    String violations = String.join(", ", result.getViolations());
                    throw new IllegalArgumentException("Input validation failed: " + violations);
                }
            }
        }
        return joinPoint.proceed();
    }

    @Around("@annotation(ValidateOutput)")
    public Object validateOutput(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof String) {
            GuardResult validation = guardService.validateOutput((String) result);
            if (validation.isBlocked()) {
                log.warn("Output validation failed: {}", validation.getReason());
                return "I'm sorry, but I can't provide a response to that request.";
            }
            return guardService.sanitizeOutput((String) result);
        }
        return result;
    }
}