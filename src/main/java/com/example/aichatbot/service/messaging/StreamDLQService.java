package com.example.aichatbot.service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamDLQService {

    @Value("${spring.redis.stream.dlq.key}")
    private String dlqKey;

    private final StringRedisTemplate redisTemplate;

    public void optimizeAndMoveToDLQ(ObjectRecord<String, String> message, String originalStream, String group) {
        log.error("Moving message {} to DLQ from stream {} group {}", message.getId(), originalStream, group);

        ObjectRecord<String, String> dlqRecord = StreamRecords.newRecord()
                .ofObject(message.getValue())
                .withStreamKey(dlqKey);

        redisTemplate.opsForStream().add(dlqRecord);

        redisTemplate.opsForStream().acknowledge(group, message);
    }
}
