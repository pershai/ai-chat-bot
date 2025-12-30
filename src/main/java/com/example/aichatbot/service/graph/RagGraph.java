package com.example.aichatbot.service.graph;

import com.example.aichatbot.service.Assistant;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.example.aichatbot.enums.RagStateName.CLARIFY;
import static com.example.aichatbot.enums.RagStateName.GENERATE;
import static com.example.aichatbot.enums.RagStateName.GRADE;
import static com.example.aichatbot.enums.RagStateName.RETRIEVE;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagGraph {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final Assistant assistant;

    public CompiledGraph<RagState> buildGraph() throws Exception {
        StateGraph<RagState> workflow = new StateGraph<>(RagState::new);

        workflow.addNode(RETRIEVE.toString(), this::retrieve);
        workflow.addNode(GRADE.toString(), this::grade);
        workflow.addNode(GENERATE.toString(), this::generate);
        workflow.addNode(CLARIFY.toString(), this::clarify);

        workflow.addEdge(START, RETRIEVE.toString());
        workflow.addEdge(RETRIEVE.toString(), GRADE.toString());

        workflow.addConditionalEdges(
                GRADE.toString(),
                state -> CompletableFuture.completedFuture(
                        Boolean.TRUE.equals(state.getIsRelevant())
                                ? GENERATE.toString()
                                : CLARIFY.toString()),
                Map.of(
                        GENERATE.toString(), GENERATE.toString(),
                        CLARIFY.toString(), CLARIFY.toString()));

        workflow.addEdge(GENERATE.toString(), END);
        workflow.addEdge(CLARIFY.toString(), END);

        return workflow.compile();
    }

    private CompletableFuture<Map<String, Object>> retrieve(RagState state) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Retrieving documents for query: {} and user: {}", state.getQuery(), state.getUserId());

            // Embed the query
            Embedding queryEmbedding = embeddingModel.embed(state.getQuery()).content();

            // Search with filter
            EmbeddingSearchRequest request = EmbeddingSearchRequest
                    .builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(MetadataFilterBuilder.metadataKey("userId")
                            .isEqualTo(state.getUserId()))
                    .maxResults(5) // Default or config
                    .minScore(0.7) // Default or config
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

            List<String> documents = result.matches().stream()
                    .map(match -> match.embedded().text())
                    .toList();

            return Map.of("documents", documents);
        });
    }

    private CompletableFuture<Map<String, Object>> grade(RagState state) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Grading relevance...");
            if (state.getDocuments() == null || state.getDocuments().isEmpty()) {
                return Map.of("isRelevant", false);
            }

            String prompt = "You are a grader. Given the user query and retrieved documents, return 'yes' if the documents are relevant, and 'no' if they are not.\n"
                            +
                            "Query: " + state.getQuery() + "\n" +
                            "Documents: " + String.join("\n", state.getDocuments());

            Result<String> result = assistant.chat("temp-grade", "You are a grader.", prompt);
            String response = result.content().trim().toLowerCase();
            boolean relevant = response.contains("yes");
            log.info("Relevance: {}", relevant);

            Map<String, Integer> usage = new java.util.HashMap<>(state.getTokenUsage());
            updateTokenUsage(usage, result.tokenUsage(), "grade");

            return Map.of("isRelevant", relevant, "tokenUsage", usage);
        });
    }

    private CompletableFuture<Map<String, Object>> generate(RagState state) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Generating answer...");
            String context = String.join("\n\n", state.getDocuments());
            String prompt = "Context:\n" + context + "\n\nQuestion: " + state.getQuery();
            dev.langchain4j.service.Result<String> result = assistant.chat(state.getConversationId(),
                    "You are a helpful assistant.", prompt);

            Map<String, Integer> usage = new java.util.HashMap<>(state.getTokenUsage());
            updateTokenUsage(usage, result.tokenUsage(), "generate");

            return Map.of("response", result.content(), "tokenUsage", usage);
        });
    }

    private CompletableFuture<Map<String, Object>> clarify(RagState state) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Generating clarification...");
            String prompt = "The user asked: " + state.getQuery() +
                            ". We could not find relevant information in our knowledge base. " +
                            "Please ask for clarification or provide a general response.";
            dev.langchain4j.service.Result<String> result = assistant.chat(state.getConversationId(),
                    "You are a helpful assistant.", prompt);

            Map<String, Integer> usage = new java.util.HashMap<>(state.getTokenUsage());
            updateTokenUsage(usage, result.tokenUsage(), "clarify");

            return Map.of("response", result.content(), "tokenUsage", usage);
        });
    }

    private void updateTokenUsage(Map<String, Integer> usageMap, TokenUsage tokenUsage,
                                  String stepPrefix) {
        if (tokenUsage == null)
            return;

        usageMap.merge("totalInputTokens", tokenUsage.inputTokenCount(), Integer::sum);
        usageMap.merge("totalOutputTokens", tokenUsage.outputTokenCount(), Integer::sum);

        usageMap.put(stepPrefix + "_input", tokenUsage.inputTokenCount());
        usageMap.put(stepPrefix + "_output", tokenUsage.outputTokenCount());
    }
}