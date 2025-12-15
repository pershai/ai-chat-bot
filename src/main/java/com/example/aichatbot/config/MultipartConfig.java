package com.example.aichatbot.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConfig {

    @Value("${spring.servlet.multipart.max-file-size}")
    long maxFileSize;
    @Value("${spring.servlet.multipart.max-request-size}")
    long maxRequestSize;
    @Value("${spring.servlet.multipart.file-size-threshold}")
    int fileSizeThreshold;
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", maxFileSize, maxRequestSize, fileSizeThreshold);
    }
}