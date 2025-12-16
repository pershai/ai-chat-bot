package com.example.aichatbot.config;

import com.example.aichatbot.exception.InfrastructureException;
import com.example.aichatbot.service.Assistant;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@Slf4j
public class AiConfig {

    @Value("${langchain4j.gemini.chat-model.api-key}")
    private String googleApiKey;
    @Value("${langchain4j.gemini.chat-model.model-name}")
    private String model;
    @Value("${langchain4j.gemini.chat-model.temperature}")
    private Double temperature;
    @Value("${langchain4j.gemini.chat-model.timeout}")
    private Integer timeout;
    @Value("${langchain4j.gemini.chat-model.max-memory-size}")
    private Integer maxMemorySize;

    @Value("${langchain4j.qdrant.host}")
    private String qdrantHost;
    @Value("${langchain4j.qdrant.port}")
    private Integer qdrantPort;
    @Value("${langchain4j.qdrant.collection-name}")
    private String collectionName;
    @Value("${langchain4j.document.splitter.max-segment-size}")
    private int maxSegmentSize;

    @Value("${langchain4j.document.splitter.max-overlap-size}")
    private int maxOverlapSize;
    @Value("${langchain4j.document.splitter.maxResults}")
    private int maxResults;

    @Value("${langchain4j.embedding.provider:huggingface}")
    private String embeddingProvider;
    @Value("${langchain4j.embedding.huggingface.api-key:}")
    private String huggingfaceApiKey;
    @Value("${langchain4j.embedding.huggingface.model-name:sentence-transformers/all-MiniLM-L6-v2}")
    private String huggingfaceModelName;
    @Value("${langchain4j.embedding.huggingface.timeout:30}")
    private Integer huggingfaceTimeout;
    @Value("${langchain4j.embedding.google.model-name:text-embedding-004}")
    private String googleEmbeddingModel;

    @Bean
    public ChatModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(googleApiKey)
                .modelName(model)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing embedding model with provider: {}", embeddingProvider);

        return switch (embeddingProvider.toLowerCase()) {
            case "google" -> GoogleAiEmbeddingModel.builder()
                    .apiKey(googleApiKey)
                    .modelName(googleEmbeddingModel)
                    .timeout(Duration.ofSeconds(timeout))
                    .build();

            case "huggingface" -> {
                if (huggingfaceApiKey == null || huggingfaceApiKey.isEmpty()) {
                    throw new IllegalStateException(
                            "HUGGINGFACE_API_KEY is required when using HuggingFace embeddings. "
                                    + "Get your API key from https://huggingface.co/settings/tokens");
                }
                log.info("Using HuggingFace model: {}", huggingfaceModelName);
                yield HuggingFaceEmbeddingModel.builder()
                        .accessToken(huggingfaceApiKey)
                        .modelId(huggingfaceModelName)
                        .timeout(Duration.ofSeconds(huggingfaceTimeout))
                        .build();
            }

            default -> throw new IllegalArgumentException(
                    "Unknown embedding provider: " + embeddingProvider +
                            ". Supported providers: google, huggingface");
        };
    }

    @Bean
    @Primary
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Initializing embedding store at {}:{} with collection '{}'",
                qdrantHost, qdrantPort, collectionName);

        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build());

        try {
            Boolean exists = client.collectionExistsAsync(collectionName).get();
            if (exists != null && !exists) {
                log.info("Creating collection '{}'", collectionName);
                client.createCollectionAsync(
                        collectionName,
                        Collections.VectorParams.newBuilder()
                                .setSize(embeddingModel().dimension())
                                .setDistance(Collections.Distance.Cosine)
                                .build())
                        .get();
            }
        } catch (Exception e) {
            log.error("Error initializing collection", e);
            throw new InfrastructureException("Qdrant", "Failed to initialize collection", e);
        }

        return QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .collectionName(collectionName)
                .useTls(false)
                .build();
    }

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {

        return EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize))
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(temperature)
                .build();
    }

    @Bean
    public Assistant assistant(ChatModel chatLanguageModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatLanguageModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(maxMemorySize)
                        .build())
                .build();
    }
}