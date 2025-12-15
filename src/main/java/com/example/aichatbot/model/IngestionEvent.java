package com.example.aichatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionEvent {
    private String jobId;
    private Integer userId;
    private List<String> filePaths;
}
