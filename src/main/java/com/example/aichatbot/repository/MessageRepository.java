package com.example.aichatbot.repository;

import com.example.aichatbot.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByConversationId(Integer conversationId);

    long countByConversationUserId(Integer userId);

    @Query("SELECT SUM(m.inputTokens) FROM Message m")
    Long sumInputTokens();

    @Query("SELECT SUM(m.outputTokens) FROM Message m")
    Long sumOutputTokens();

    @Query("SELECT SUM(m.inputTokens) FROM Message m WHERE m.conversation.userId = :userId")
    Long sumInputTokensByUserId(Integer userId);

    @Query("SELECT SUM(m.outputTokens) FROM Message m WHERE m.conversation.userId = :userId")
    Long sumOutputTokensByUserId(Integer userId);
}
