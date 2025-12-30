package com.example.aichatbot.controller;

import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.service.StatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Tag(name = "Statistics", description = "System statistics")
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserRepository userRepository;

    public StatisticsController(StatisticsService statisticsService, UserRepository userRepository) {
        this.statisticsService = statisticsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getStatistics(Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            String userId = userRepository.findByUsername(username)
                    .map(com.example.aichatbot.model.User::getId)
                    .orElse(null);
            if (userId != null) {
                var stats = statisticsService.getUserStatistics(userId);
                System.out.println("DEBUG: Stats for user " + userId + ": " + stats);
                return ResponseEntity.ok(stats);
            }
        }
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
