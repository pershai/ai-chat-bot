package com.example.aichatbot.service;

import com.example.aichatbot.dto.BotConfigDto;
import com.example.aichatbot.security.LlmGuardService;
import com.example.aichatbot.service.graph.RagState;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private CompiledGraph<RagState> ragGraphRunner;
    @Mock
    private ConversationService conversationService;
    @Mock
    private LlmGuardService guardService;

    @InjectMocks
    private ChatService chatService;

    private BotConfigDto botConfig;

    @BeforeEach
    void setUp() {
        botConfig = new BotConfigDto("friendly");
    }

    @Test
    void processChat_Success() {
        // Arrange
        String userId = "1";
        Long conversationId = 100L;
        String message = "Hello AI";
        String expectedResponse = "Hello User";

        Map<String, Object> stateData = new HashMap<>();
        stateData.put("response", expectedResponse);
        RagState mockState = new RagState(stateData);

        when(ragGraphRunner.invoke(anyMap())).thenReturn(Optional.of(mockState));

        // Act
        String result = chatService.processChat(userId, conversationId, message, botConfig);

        // Assert
        assertEquals(expectedResponse, result);
        verify(conversationService).addMessage(conversationId, "user", message);
        verify(conversationService).addMessage(conversationId, "assistant", expectedResponse, 0, 0);
        verify(ragGraphRunner).invoke(argThat(map -> map.get("query").equals(message)));
    }

    @Test
    void processChat_EmptyGraphResponse_ReturnsError() {
        // Arrange
        String userId = "1";
        Long conversationId = 100L;
        String message = "Hello";

        when(ragGraphRunner.invoke(anyMap())).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class,
                () -> chatService.processChat(userId, conversationId, message, botConfig));
        assertTrue(exception.getMessage().contains("Graph returned empty state"));
    }

    @Test
    void processChat_GraphError_ThrowsException() {
        // Arrange
        String userId = "1";
        Long conversationId = 100L;
        String message = "Error me";

        when(ragGraphRunner.invoke(anyMap())).thenThrow(new RuntimeException("Graph Down"));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class,
                () -> chatService.processChat(userId, conversationId, message, botConfig));
        assertTrue(exception.getMessage().contains("Failed to process chat message"));
    }

    @Test
    void processChat_NullResponseInState_UsesFallback() {
        // Arrange
        String userId = "1";
        Long conversationId = 100L;
        String message = "Hello";

        Map<String, Object> stateData = new HashMap<>();
        stateData.put("response", null); // No response in state
        RagState mockState = new RagState(stateData);

        when(ragGraphRunner.invoke(anyMap())).thenReturn(Optional.of(mockState));

        // Act
        String result = chatService.processChat(userId, conversationId, message, botConfig);

        // Assert
        assertEquals("I encountered an error processing your request.", result);
        verify(conversationService).addMessage(conversationId, "user", message);
        verify(conversationService).addMessage(conversationId, "assistant",
                "I encountered an error processing your request.", 0, 0);
    }
}
