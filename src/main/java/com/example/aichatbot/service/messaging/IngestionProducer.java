package com.example.aichatbot.service.messaging;

import com.example.aichatbot.config.StreamConfig;
import com.example.aichatbot.model.IngestionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final StreamConfig streamConfig;

    public void publish(IngestionEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .ofObject(json)
                    .withStreamKey(streamConfig.getKey());

            redisTemplate.opsForStream().add(record);
            log.info("Job {}: Produced ingestion event to stream {}", event.getJobId(),
                    streamConfig.getKey());
        } catch (Exception e) {
            log.error("Failed to publish ingestion event", e);
            throw new RuntimeException("Failed to publish ingestion event", e);
        }
    }
}
