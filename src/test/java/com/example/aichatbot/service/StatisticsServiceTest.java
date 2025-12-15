package com.example.aichatbot.service;

import com.example.aichatbot.dto.StatisticsDto;
import com.example.aichatbot.repository.ConversationRepository;
import com.example.aichatbot.repository.DocumentRepository;
import com.example.aichatbot.repository.MessageRepository;
import com.example.aichatbot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void getStatistics_ReturnsCorrectCounts() {
        // Arrange
        when(userRepository.count()).thenReturn(10L);
        when(conversationRepository.count()).thenReturn(20L);
        when(messageRepository.count()).thenReturn(100L);
        when(documentRepository.count()).thenReturn(5L);
        when(conversationRepository.countByUpdatedAtAfter(any(LocalDateTime.class))).thenReturn(2L);

        // Act
        StatisticsDto stats = statisticsService.getStatistics();

        // Assert
        assertEquals(10L, stats.totalUsers());
        assertEquals(20L, stats.totalConversations());
        assertEquals(100L, stats.totalMessages());
        assertEquals(5L, stats.totalDocuments());
        assertEquals(2L, stats.activeConversations24h());
    }
}
