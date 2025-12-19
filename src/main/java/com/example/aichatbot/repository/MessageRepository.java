package com.example.aichatbot.repository;

import com.example.aichatbot.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationId(Long conversationId);

    long countByConversationUserId(String userId);

    @Query("SELECT SUM(m.inputTokens) FROM Message m")
    Long sumInputTokens();

    @Query("SELECT SUM(m.outputTokens) FROM Message m")
    Long sumOutputTokens();

    @Query("SELECT SUM(m.inputTokens) FROM Message m WHERE m.conversation.userId = :userId")
    Long sumInputTokensByUserId(String userId);

    @Query("SELECT SUM(m.outputTokens) FROM Message m WHERE m.conversation.userId = :userId")
    Long sumOutputTokensByUserId(String userId);
}
