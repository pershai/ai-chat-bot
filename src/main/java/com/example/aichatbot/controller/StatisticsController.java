package com.example.aichatbot.controller;

import com.example.aichatbot.dto.StatisticsDto;
import com.example.aichatbot.dto.UserStatisticsDto;
import com.example.aichatbot.service.StatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics", description = "System statistics")
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ResponseEntity<?> getStatistics(
            @RequestParam(required = false) Integer userId) {
        if (userId != null) {
            return ResponseEntity.ok(statisticsService.getUserStatistics(userId));
        }
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
