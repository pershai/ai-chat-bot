package com.example.aichatbot.service;

import com.example.aichatbot.dto.StatisticsDto;
import com.example.aichatbot.dto.UserStatisticsDto;
import com.example.aichatbot.repository.ConversationRepository;
import com.example.aichatbot.repository.DocumentRepository;
import com.example.aichatbot.repository.MessageRepository;
import com.example.aichatbot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentRepository documentRepository;

    public StatisticsService(UserRepository userRepository,
                             ConversationRepository conversationRepository,
                             MessageRepository messageRepository,
                             DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.documentRepository = documentRepository;
    }

    public StatisticsDto getStatistics() {
        long totalUsers = userRepository.count();
        long totalConversations = conversationRepository.count();
        long totalMessages = messageRepository.count();
        long totalDocuments = documentRepository.count();
        long activeConversations24h = conversationRepository
                .countByUpdatedAtAfter(LocalDateTime.now().minusHours(24));

        Long totalInputTokens = messageRepository.sumInputTokens();
        Long totalOutputTokens = messageRepository.sumOutputTokens();
        long totalTokens = (totalInputTokens != null ? totalInputTokens : 0)
                           + (totalOutputTokens != null ? totalOutputTokens : 0);

        return new StatisticsDto(
                totalUsers,
                totalConversations,
                totalMessages,
                totalDocuments,
                activeConversations24h,
                totalTokens);
    }

    /**
     * Get statistics for a specific user
     *
     * @param userId the user ID
     * @return user-specific statistics
     */
    public UserStatisticsDto getUserStatistics(String userId) {
        long userConversations = conversationRepository.countByUserId(userId);
        long userMessages = messageRepository.countByConversationUserId(userId);
        long totalMessages = messageRepository.count(); // Keep for context
        long totalDocuments = documentRepository.count(); // Shared knowledge base
        long userActive24h = conversationRepository.countByUserIdAndUpdatedAtAfter(
                userId, LocalDateTime.now().minusHours(24));

        Long userInputTokens = messageRepository.sumInputTokensByUserId(userId);
        Long userOutputTokens = messageRepository.sumOutputTokensByUserId(userId);
        long userTotalTokens = (userInputTokens != null ? userInputTokens : 0)
                               + (userOutputTokens != null ? userOutputTokens : 0);

        return new UserStatisticsDto(
                userConversations,
                userMessages,
                totalMessages,
                totalDocuments,
                userActive24h,
                userTotalTokens);
    }
}
