package com.example.aichatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
@Data
public class FileStorageConfig {
    private List<String> allowedExtensions;
    private int maxFilenameLength;
    private String storagePath;
}