package com.example.aichatbot.service.graph;

import com.example.aichatbot.service.Assistant;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagGraphTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private Assistant assistant;

    private CompiledGraph<RagState> graph;
    private static final String CONV_ID = "123";
    private static final String USER_ID = "test-user";

    @BeforeEach
    void setUp() throws Exception {
        RagGraph ragGraph = new RagGraph(embeddingStore, embeddingModel, assistant);
        graph = ragGraph.buildGraph();
    }

    @Test
    void testFullGraphProcess() {
        // Arrange
        String query = "What is the capital of France?";
        String documentText = "Paris is the capital of France.";

        Embedding queryEmbedding = new Embedding(new float[384]);
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(queryEmbedding));

        // Mock EmbeddingStore
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id1", queryEmbedding,
                TextSegment.from(documentText));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        when(assistant.chat(eq("temp-grade"), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("yes")
                        .tokenUsage(new TokenUsage(10, 5))
                        .build());

        when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("The capital of France is Paris.")
                        .tokenUsage(new TokenUsage(50, 20))
                        .build());

        // Act
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);
        inputs.put("conversationId", CONV_ID);
        inputs.put("userId", USER_ID);

        Optional<RagState> optionalState = graph.invoke(inputs);

        // Assert
        assertTrue(optionalState.isPresent());
        RagState finalState = optionalState.get();
        assertEquals("The capital of France is Paris.", finalState.getResponse());
        assertTrue(finalState.getIsRelevant());

        Map<String, Integer> usage = finalState.getTokenUsage();
        assertEquals(60, usage.get("totalInputTokens"));
        assertEquals(25, usage.get("totalOutputTokens"));
    }

    @Test
    void testGraphWithIrrelevantDocuments() throws Exception {
        // Arrange
        String query = "What is the capital of France?";

        Embedding queryEmbedding = new Embedding(new float[384]);
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(queryEmbedding));

        // Mock EmbeddingStore (return irrelevant doc or just something)
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "id2", queryEmbedding,
                TextSegment.from("Irrelevant text"));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        when(assistant.chat(eq("temp-grade"), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("no")
                        .tokenUsage(new TokenUsage(10, 5))
                        .build());

        when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("I couldn't find relevant information. Could you clarify?")
                        .tokenUsage(new TokenUsage(30, 15))
                        .build());

        // Act
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);
        inputs.put("conversationId", CONV_ID);
        inputs.put("userId", USER_ID);

        Optional<RagState> optionalState = graph.invoke(inputs);

        // Assert
        assertTrue(optionalState.isPresent());
        RagState finalState = optionalState.get();
        assertFalse(finalState.getIsRelevant());
        assertTrue(finalState.getResponse().contains("clarify"));
    }

    @Test
    void testGraphWithNoDocuments() throws Exception {
        // Arrange
        String query = "Searching for something non-existent";

        Embedding queryEmbedding = new Embedding(new float[384]);
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(queryEmbedding));

        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));

        when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("No documents found.")
                        .tokenUsage(new TokenUsage(5, 2))
                        .build());

        // Act
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);
        inputs.put("conversationId", CONV_ID);
        inputs.put("userId", USER_ID);

        Optional<RagState> optionalState = graph.invoke(inputs);

        // Assert
        assertTrue(optionalState.isPresent());
        RagState finalState = optionalState.get();
        assertFalse(finalState.getIsRelevant());
        assertEquals("No documents found.", finalState.getResponse());
    }
}