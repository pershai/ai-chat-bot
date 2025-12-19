package com.example.aichatbot.repository;

import com.example.aichatbot.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(String userId);

    void deleteByUserId(String userId);
}
