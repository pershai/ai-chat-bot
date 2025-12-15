package com.example.aichatbot.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class IngestionJob {
    private String jobId;
    private JobStatus status;
    private int totalFiles;
    private int processedFiles;
    private List<String> errors = new ArrayList<>();
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED
    }
}