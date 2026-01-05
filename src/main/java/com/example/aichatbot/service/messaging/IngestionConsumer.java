package com.example.aichatbot.service.messaging;

import com.example.aichatbot.config.StreamConfig;
import com.example.aichatbot.model.IngestionEvent;
import com.example.aichatbot.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final DocumentService documentService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final StreamDLQService dlqService;
    private final StreamConfig streamConfig;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {

        int attempts = 0;
        int maxRetries = streamConfig.getMaxRetries(); // Assuming added to StreamConfig, or use default if not present
        boolean success = false;

        while (attempts < maxRetries && !success) {
            try {
                processMessage(message);
                success = true;
                redisTemplate.opsForStream().acknowledge(streamConfig.getGroup(), message);
            } catch (Exception e) {
                attempts++;
                log.warn("Attempt {}/{} failed for message {}", attempts, maxRetries, message.getId(), e);
                if (attempts >= maxRetries) {
                    log.error("Max retries reached for message {}. Moving to DLQ.", message.getId());
                    dlqService.optimizeAndMoveToDLQ(message, streamConfig.getKey(),
                            streamConfig.getGroup());
                }
            }
        }
    }

    private void processMessage(ObjectRecord<String, String> message) throws Exception {
        IngestionEvent event = objectMapper.readValue(message.getValue(), IngestionEvent.class);
        log.info("Job {}: Consumed event for user {}", event.getJobId(), event.getUserId());

        List<Path> paths = event.getFilePaths().stream()
                .map(Paths::get)
                .collect(Collectors.toList());

        documentService.ingestFiles(event.getJobId(), paths, event.getUserId());
    }
}
