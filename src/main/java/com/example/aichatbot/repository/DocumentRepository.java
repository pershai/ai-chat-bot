package com.example.aichatbot.repository;

import com.example.aichatbot.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
    List<Document> findByUserId(Integer userId);

    Optional<Document> findByIdAndUserId(Integer id, Integer userId);
}
