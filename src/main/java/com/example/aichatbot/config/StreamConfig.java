package com.example.aichatbot.config;

import com.example.aichatbot.exception.ResourceNotFoundException;
import com.example.aichatbot.service.messaging.IngestionConsumer;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "spring.redis.stream.ingestion")
@Getter
@Setter
public class StreamConfig {
    private String key;
    private String group;
    private int maxRetries = 3;
    private ConsumerConfig consumer;

    @Data
    public static class ConsumerConfig {
        private String prefix;
        private long pollTimeoutMs;
        private boolean autoStartup;
    }

    private static String hostId = getHostId();

    @Bean
    public Subscription subscription(RedisConnectionFactory redisConnectionFactory,
            IngestionConsumer ingestionConsumer) {
        createGroupIfNotExists(redisConnectionFactory);

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofMillis(consumer.getPollTimeoutMs()))
                .targetType(String.class)
                .build();

        var listenerContainer = StreamMessageListenerContainer
                .create(redisConnectionFactory, options);

        String consumerId = generateConsumerId();
        log.info("Creating Redis Stream consumer with ID: {} for group: {} on stream: {}",
                consumerId, group, key);

        var subscription = listenerContainer.receive(
                Consumer.from(group, consumerId),
                StreamOffset.create(key, ReadOffset.lastConsumed()),
                ingestionConsumer);

        if (consumer.isAutoStartup()) {
            listenerContainer.start();
        }

        return subscription;
    }

    private String generateConsumerId() {
        return consumer.getPrefix() + hostId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String getHostId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new ResourceNotFoundException("Could not determine host name", e);
        }
    }

    private void createGroupIfNotExists(RedisConnectionFactory redisConnectionFactory) {
        try {
            redisConnectionFactory.getConnection()
                    .streamCommands()
                    .xGroupCreate(key.getBytes(), group, ReadOffset.from("0-0"));
            log.info("Created Redis Stream group: {} for key: {}", group, key);
        } catch (RedisSystemException e) {
            log.debug("Redis Stream group {} already exists for key: {}", group, key);
        }
    }
}