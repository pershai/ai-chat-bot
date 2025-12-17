package com.example.aichatbot.service.graph;

import com.example.aichatbot.service.Assistant;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagGraphTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private Assistant assistant;

    @InjectMocks
    private RagGraph ragGraph;

    private CompiledGraph<RagState> graph;
    private static final String CONV_ID = "123";
    private static final Integer USER_ID = 42;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(ragGraph, "collectionName", "test-collection");
        ReflectionTestUtils.setField(ragGraph, "maxResults", 5);
        ReflectionTestUtils.setField(ragGraph, "minScore", 0.7);
        graph = ragGraph.buildGraph();
    }

    @Test
    void testGraphRetrievalFiltersByUserId() throws Exception {
        // Arrange
        String query = "What is my secret?";
        TextSegment segment = TextSegment.from("Your secret is 1234.");
        Embedding embedding = new Embedding(new float[]{0.1f, 0.2f});

        Response<Embedding> embeddingResponse = Response.from(embedding);
        when(embeddingModel.embed(query)).thenReturn(embeddingResponse);

        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id", embedding, segment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        when(assistant.chat(eq("temp-grade"), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("yes")
                        .tokenUsage(new TokenUsage(10, 5))
                        .finishReason(FinishReason.STOP)
                        .build());

        when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                .thenReturn(Result.<String>builder()
                        .content("Secret is 1234.")
                        .tokenUsage(new TokenUsage(10, 5))
                        .finishReason(FinishReason.STOP)
                        .build());

        // Act
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", query);
        inputs.put("conversationId", CONV_ID);
        inputs.put("userId", USER_ID);

        Optional<RagState> optionalResult = graph.invoke(inputs);

        // Assert
        assertTrue(optionalResult.isPresent());
        RagState result = optionalResult.get();
        assertEquals("Secret is 1234.", result.getResponse());

        // Verify filter
        ArgumentCaptor<EmbeddingSearchRequest> captor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());

        EmbeddingSearchRequest request = captor.getValue();
        Filter filter = request.filter();
        assertNotNull(filter);
        String filterString = filter.toString();
        assertTrue(filterString.contains("userId"), "Filter should contain userId key");
        assertTrue(filterString.contains(String.valueOf(USER_ID)), "Filter should contain userId value");
    }
}