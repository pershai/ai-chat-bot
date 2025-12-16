package com.example.aichatbot.controller;

import com.example.aichatbot.dto.StatisticsDto;
import com.example.aichatbot.dto.UserStatisticsDto;
import com.example.aichatbot.security.JwtAuthenticationFilter;
import com.example.aichatbot.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getStatistics_ReturnsStats() throws Exception {
        // Arrange
        StatisticsDto mockStats = new StatisticsDto(10, 20, 100, 5, 2, 500);

        when(statisticsService.getStatistics()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.totalConversations").value(20));
    }

    @Test
    void getUserStatistics_ReturnsUserStats() throws Exception {
        // Arrange
        Integer userId = 123;
        UserStatisticsDto mockUserStats = new UserStatisticsDto(5, 50, 1000, 10, 1, 250);

        when(statisticsService.getUserStatistics(userId)).thenReturn(mockUserStats);

        // Act & Assert
        mockMvc.perform(get("/api/v1/statistics")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myConversations").value(5))
                .andExpect(jsonPath("$.myMessages").value(50))
                .andExpect(jsonPath("$.totalMessages").value(1000))
                .andExpect(jsonPath("$.totalDocuments").value(10))
                .andExpect(jsonPath("$.myActiveConversations24h").value(1))
                .andExpect(jsonPath("$.myTotalTokens").value(250));
    }
}
