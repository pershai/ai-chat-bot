package com.example.aichatbot.service;

import com.example.aichatbot.dto.DocumentDto;
import com.example.aichatbot.dto.UpdateDocumentRequest;
import com.example.aichatbot.exception.DocumentUpdateException;
import com.example.aichatbot.exception.ResourceNotFoundException;
import com.example.aichatbot.repository.DocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final ChatModel chatModel;
    private final EmbeddingStoreIngestor ingestor;
    private final JobService jobService;
    private final DocumentRepository documentRepository;
    private final com.example.aichatbot.service.storage.FileStorageService fileStorageService;
    private final QdrantClient qdrantClient;

    @Value("${langchain4j.qdrant.collection-name}")
    private String collectionName;

    public void ingestFiles(String jobId, List<Path> filePaths, Integer userId) {
        log.info("Job {}: Starting ingestion for user {}...", jobId, userId);

        for (Path path : filePaths) {
            String filename = path.toString();
            try {
                processSingleFile(filename, userId);
                jobService.updateProgress(jobId);
            } catch (Exception e) {
                log.error("Job {}: Failed to process file {}", jobId, filename, e);
                jobService.addError(jobId, "File " + filename + ": " + e.getMessage());
            } finally {
                fileStorageService.delete(filename);
            }
        }

        jobService.markCompleted(jobId);
        log.info("Job {}: Completed.", jobId);
    }

    private void processSingleFile(String filename, Integer userId) throws Exception {
        try (InputStream inputStream = fileStorageService.load(filename)) {
            ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
            Document document = parser.parse(inputStream);
            document.metadata().put("filename", fileStorageService.resolve(filename).getFileName().toString());
            document.metadata().put("userId", userId.toString());
            ingestor.ingest(document);

            com.example.aichatbot.model.Document dbDocument = new com.example.aichatbot.model.Document();
            dbDocument.setFilename(fileStorageService.resolve(filename).getFileName().toString());
            dbDocument.setUserId(userId);
            dbDocument.setFileType(getFileExtension(filename));
            dbDocument.setUploadDate(LocalDateTime.now());

            try {
                String text = document.text();
                if (text != null && !text.isBlank()) {
                    String limitedText = text.substring(0, Math.min(text.length(), 2000));
                    String prompt = "Summarize the following text in 50 words or less:\n\n" + limitedText;
                    String summary = chatModel.chat(prompt);
                    dbDocument.setSummary(summary);
                }
            } catch (Exception e) {
                log.warn("Failed to generate summary for document {}", filename, e);
            }
            documentRepository.save(dbDocument);
            log.info("Saved document {} for user {}", filename, userId);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toUpperCase() : "UNKNOWN";
    }

    public List<DocumentDto> getDocuments(Integer userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(doc -> DocumentDto.builder()
                        .id(doc.getId())
                        .filename(doc.getFilename())
                        .fileType(doc.getFileType())
                        .uploadDate(doc.getUploadDate())
                        .summary(doc.getSummary())
                        .build())
                .collect(Collectors.toList());
    }

    public DocumentDto updateDocument(Integer documentId, Integer userId, UpdateDocumentRequest request) {
        com.example.aichatbot.model.Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (document.getSummary() != null && !document.getSummary().isBlank()) {
            throw new DocumentUpdateException("Cannot update document with existing summary");
        }

        if (request.getFilename() != null && !request.getFilename().isBlank()) {
            document.setFilename(request.getFilename());
        }

        String summaryToSet = request.getSummary();
        if (summaryToSet == null || summaryToSet.isBlank()) {
            // Regenerate summary if not provided
            summaryToSet = regenerateSummary(document.getFilename());
        }

        if (summaryToSet != null) {
            document.setSummary(summaryToSet);
        }

        com.example.aichatbot.model.Document savedDoc = documentRepository.save(document);

        return DocumentDto.builder()
                .id(savedDoc.getId())
                .filename(savedDoc.getFilename())
                .fileType(savedDoc.getFileType())
                .uploadDate(savedDoc.getUploadDate())
                .summary(savedDoc.getSummary())
                .build();
    }

    public String regenerateSummary(String filename) {
        String text = fetchTextFromQdrant(filename);
        if (text != null && !text.isBlank()) {
            String limitedText = text.substring(0, Math.min(text.length(), 2000));
            String prompt = "Summarize the following text in 50 words or less:\n\n" + limitedText;
            try {
                return chatModel.chat(prompt);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                log.warn("Failed to generate summary for document {} - {}", filename, errorMessage);
            }
        }
        return null;
    }

    private String fetchTextFromQdrant(String filename) {
        try {
            List<Points.RetrievedPoint> points = qdrantClient.scrollAsync(
                            Points.ScrollPoints.newBuilder()
                                    .setCollectionName(collectionName)
                                    .setLimit(10)
                                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                                    .setFilter(
                                            Common.Filter.newBuilder()
                                                    .addMust(ConditionFactory.matchText("filename", filename))
                                                    .build())
                                    .build())
                    .get()
                    .getResultList();

            return points.stream()
                    .map(p -> p.getPayloadMap().get("text_segment")) // LangChain4j default key
                    .filter(Objects::nonNull)
                    .map(JsonWithInt.Value::getStringValue)
                    .collect(Collectors.joining(" "));
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to fetch segments from Qdrant for file {}", filename, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Failed to fetch segments from Qdrant for file {}", filename, e);
        }
        return null;
    }
}
