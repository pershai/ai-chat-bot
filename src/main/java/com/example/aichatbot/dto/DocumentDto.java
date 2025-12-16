package com.example.aichatbot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentDto {
    private Integer id;
    private String filename;
    private String fileType;
    private LocalDateTime uploadDate;
    private String summary;
}
