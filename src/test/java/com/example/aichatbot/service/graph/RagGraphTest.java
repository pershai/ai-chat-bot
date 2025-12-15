package com.example.aichatbot.service.graph;

import com.example.aichatbot.service.Assistant;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagGraphTest {

        @Mock
        private ContentRetriever contentRetriever;

        @Mock
        private Assistant assistant;

        private CompiledGraph<RagState> graph;
        private static final String CONV_ID = "123";

        @BeforeEach
        void setUp() throws Exception {
                RagGraph ragGraph = new RagGraph(contentRetriever, assistant);
                graph = ragGraph.buildGraph();
        }

        @Test
        void testGraphWithRelevantDocuments() throws Exception {
                // Arrange
                String query = "What is the capital of France?";
                String documentText = "Paris is the capital of France.";
                Content content = Content.from(documentText);

                when(contentRetriever.retrieve(any(Query.class)))
                                .thenReturn(List.of(content));
                when(assistant.chat(eq("temp-grade"), anyString(), anyString()))
                                .thenReturn(dev.langchain4j.service.Result.<String>builder()
                                                .content("yes")
                                                .tokenUsage(new dev.langchain4j.model.output.TokenUsage(10, 5))
                                                .finishReason(dev.langchain4j.model.output.FinishReason.STOP)
                                                .build());
                when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                                .thenReturn(dev.langchain4j.service.Result.<String>builder()
                                                .content("The capital of France is Paris.")
                                                .tokenUsage(new dev.langchain4j.model.output.TokenUsage(50, 20))
                                                .finishReason(dev.langchain4j.model.output.FinishReason.STOP)
                                                .build());

                // Act
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("query", query);
                inputs.put("conversationId", CONV_ID);
                Optional<RagState> optionalResult = graph.invoke(inputs);

                // Assert
                assertTrue(optionalResult.isPresent());
                RagState result = optionalResult.get();
                assertEquals("The capital of France is Paris.", result.getResponse());
                assertTrue(result.getIsRelevant());

                Map<String, Integer> usage = result.getTokenUsage();
                assertEquals(60, usage.get("totalInputTokens"));
                assertEquals(25, usage.get("totalOutputTokens"));

                verify(contentRetriever).retrieve(any(Query.class));
                verify(assistant).chat(eq("temp-grade"), anyString(), anyString());
                verify(assistant).chat(eq(CONV_ID), anyString(), anyString());
        }

        @Test
        void testGraphWithIrrelevantDocuments() throws Exception {
                // Arrange
                String query = "What is the capital of France?";
                Content content = Content.from("Some unrelated text");

                when(contentRetriever.retrieve(any(Query.class)))
                                .thenReturn(List.of(content));
                when(assistant.chat(eq("temp-grade"), anyString(), anyString()))
                                .thenReturn(dev.langchain4j.service.Result.<String>builder()
                                                .content("no")
                                                .tokenUsage(new dev.langchain4j.model.output.TokenUsage(10, 5))
                                                .finishReason(dev.langchain4j.model.output.FinishReason.STOP)
                                                .build());
                when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                                .thenReturn(dev.langchain4j.service.Result.<String>builder()
                                                .content("I couldn't find relevant information. Could you clarify your question?")
                                                .tokenUsage(new dev.langchain4j.model.output.TokenUsage(30, 15))
                                                .finishReason(dev.langchain4j.model.output.FinishReason.STOP)
                                                .build());

                // Act
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("query", query);
                inputs.put("conversationId", CONV_ID);
                Optional<RagState> optionalResult = graph.invoke(inputs);

                // Assert
                assertTrue(optionalResult.isPresent());
                RagState result = optionalResult.get();
                String response = result.getResponse();
                assertTrue(response.contains("clarify"));
                assertFalse(result.getIsRelevant());
                verify(contentRetriever).retrieve(any(Query.class));
                verify(assistant).chat(eq("temp-grade"), anyString(), anyString());
                verify(assistant).chat(eq(CONV_ID), anyString(), anyString());
        }

        @Test
        void testGraphWithNoDocuments() throws Exception {
                // Arrange
                String query = "What is the capital of France?";

                when(contentRetriever.retrieve(any(Query.class)))
                                .thenReturn(List.of());
                when(assistant.chat(eq(CONV_ID), anyString(), anyString()))
                                .thenReturn(dev.langchain4j.service.Result.<String>builder()
                                                .content("I couldn't find relevant information. Could you clarify your question?")
                                                .tokenUsage(new dev.langchain4j.model.output.TokenUsage(10, 5))
                                                .finishReason(dev.langchain4j.model.output.FinishReason.STOP)
                                                .build());

                // Act
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("query", query);
                inputs.put("conversationId", CONV_ID);
                Optional<RagState> optionalResult = graph.invoke(inputs);

                // Assert
                assertTrue(optionalResult.isPresent());
                RagState result = optionalResult.get();
                String response = result.getResponse();
                assertTrue(response.contains("clarify") || response.contains("couldn't find"));
                assertFalse(result.getIsRelevant());
                verify(contentRetriever).retrieve(any(Query.class));
                verify(assistant).chat(eq(CONV_ID), anyString(), anyString());
        }

        @Test
        void testGraphWithErrorInRetrieval() {
                // Arrange
                String query = "What is the capital of France?";

                when(contentRetriever.retrieve(any(Query.class)))
                                .thenThrow(new RuntimeException("Retrieval failed"));

                // Act & Assert
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("query", query);
                inputs.put("conversationId", CONV_ID);

                assertThrows(RuntimeException.class, () -> {
                        graph.invoke(inputs);
                });
        }
}