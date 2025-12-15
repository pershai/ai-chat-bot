package com.example.aichatbot.repository;

import com.example.aichatbot.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Integer userId);

    long countByUpdatedAtAfter(java.time.LocalDateTime timestamp);

    long countByUserId(Integer userId);

    long countByUserIdAndUpdatedAtAfter(Integer userId, java.time.LocalDateTime timestamp);
}
