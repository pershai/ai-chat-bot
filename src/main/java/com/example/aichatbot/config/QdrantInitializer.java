package com.example.aichatbot.config;

import com.example.aichatbot.exception.InfrastructureException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class QdrantInitializer {

    private static final Logger log = LoggerFactory.getLogger(QdrantInitializer.class);

    @Value("${langchain4j.qdrant.host}")
    private String host;

    @Value("${langchain4j.qdrant.port}")
    private int port;

    @Value("${langchain4j.qdrant.collection-name}")
    private String collectionName;

    @Autowired
    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build());

        try {
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new IllegalStateException(
                        "Qdrant collection name must be configured via 'langchain4j.qdrant.collection-name'");
            }

            List<String> collections = client.listCollectionsAsync().get();
            boolean exists = collections != null && collections.contains(collectionName);

            if (exists) {
                log.info("Qdrant collection '{}' already exists.", collectionName);
            } else {
                log.info("Creating Qdrant collection '{}'...", collectionName);
                VectorParams params = VectorParams.newBuilder()
                        .setSize(embeddingModel.dimension()) // Dimension for AllMiniLmL6V2
                        .setDistance(Distance.Cosine)
                        .build();

                client.createCollectionAsync(collectionName, params);
                log.info("Successfully created Qdrant collection '{}'.", collectionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to initialize Qdrant collection", e);
            throw new InfrastructureException("Qdrant", "Failed to initialize collection", e);
        } finally {
            client.close();
        }
    }
}
