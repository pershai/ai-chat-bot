package com.example.aichatbot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
@Data
public class FileStorageConfig {
    @Value("${app.file-storage.allowed-extensions}")
    private List<String> allowedExtensions;
    @Value("${app.file-storage.max-filename-length}")
    private int maxFilenameLength;
    @Value("${app.file-storage.storage-path}")
    private String storagePath;

}