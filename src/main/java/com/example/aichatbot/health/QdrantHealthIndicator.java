package com.example.aichatbot.health;

import io.qdrant.client.QdrantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component

@RequiredArgsConstructor

@Slf4j
public class QdrantHealthIndicator implements HealthIndicator {

    private final QdrantClient client;

    @Override
    public Health health() {
        try {
            client.listCollectionsAsync().get(5, TimeUnit.SECONDS);
            return Health.up().withDetail("database", "Qdrant Vector Store").build();
        } catch (Exception e) {
            log.error("Qdrant health check failed", e);
            return Health.down(e).withDetail("database", "Qdrant Vector Store").build();
        }
    }
}

