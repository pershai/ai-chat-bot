package com.example.aichatbot.service;

import com.example.aichatbot.config.StreamConfig;
import com.example.aichatbot.model.IngestionEvent;
import com.example.aichatbot.service.messaging.IngestionConsumer;
import com.example.aichatbot.service.messaging.StreamDLQService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchitecturalTests {

    @Mock
    private DocumentService documentService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private StreamOperations<String, String, String> streamOperations;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StreamDLQService dlqService;

    @InjectMocks
    private IngestionConsumer ingestionConsumer;
    @InjectMocks
    private StreamConfig streamConfig;
    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setKey("test-stream");
        streamConfig.setGroup("test-group");

        ReflectionTestUtils.setField(ingestionConsumer, "streamConfig", streamConfig);
    }

    @Test
    void ingestionConsumer_MaxRetries_MovesToDLQ() throws Exception {
        // Arrange
        String json = "{\"jobId\":\"job1\",\"userId\":1,\"filePaths\":[]}";
        ObjectRecord<String, String> message = mock(ObjectRecord.class);
        when(message.getValue()).thenReturn(json);
        when(message.getId())
                .thenReturn(org.springframework.data.redis.connection.stream.RecordId.of("1234567890123-0"));

        IngestionEvent event = new IngestionEvent("job1", "1", List.of());
        when(objectMapper.readValue(json, IngestionEvent.class)).thenReturn(event);

        doThrow(new RuntimeException("Simulated Processing Failure"))
                .when(documentService).ingestFiles(any(), any(), any());

        // Act
        ingestionConsumer.onMessage(message);

        // Assert
        verify(documentService, times(3)).ingestFiles(any(), any(), any());

        verify(dlqService).optimizeAndMoveToDLQ(
                eq(message),
                eq("test-stream"),
                eq("test-group"));
    }

    @Test
    void chatService_Fallback_ReturnsSafeMessage() {
        // Act
        String result = chatService.processChatFallback("1", 1L, "msg", null, new RuntimeException("Circuit Open"));

        // Assert
        assertEquals("The AI service is currently unavailable. Please try again later.", result);
    }
}
