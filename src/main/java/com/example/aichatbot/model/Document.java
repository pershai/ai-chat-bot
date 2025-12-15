package com.example.aichatbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate = LocalDateTime.now();

    // Optional: One-to-Many relationship mapping if needed later
    // @ManyToOne
    // @JoinColumn(name = "user_id", insertable = false, updatable = false)
    // private User user;
}
