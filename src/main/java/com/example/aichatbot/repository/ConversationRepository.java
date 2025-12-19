package com.example.aichatbot.repository;

import com.example.aichatbot.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    long countByUpdatedAtAfter(LocalDateTime timestamp);

    long countByUserId(String userId);

    long countByUserIdAndUpdatedAtAfter(String userId, LocalDateTime timestamp);

    void deleteByUserId(String userId);
}
