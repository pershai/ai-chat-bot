package com.example.aichatbot.service;

import com.example.aichatbot.dto.UpdateDocumentRequest;
import com.example.aichatbot.exception.DocumentUpdateException;
import com.example.aichatbot.exception.ResourceNotFoundException;
import com.example.aichatbot.repository.DocumentRepository;
import com.example.aichatbot.service.storage.FileStorageService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private EmbeddingStoreIngestor ingestor;

    @Mock
    private ChatModel chatModel;

    @Mock
    private JobService jobService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private io.qdrant.client.QdrantClient qdrantClient;

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    Path tempDir;

    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test file
        testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");

        lenient().when(fileStorageService.resolve(anyString()))
                .thenReturn(testFile);
        lenient().doNothing().when(fileStorageService).delete(anyString());
    }

    @Test
    void ingestFiles_ValidFiles_ProcessesSuccessfully() throws Exception {
        // Arrange
        List<Path> files = List.of(testFile);
        String jobId = "test-job-123";

        when(fileStorageService.load(anyString()))
                .thenReturn(Files.newInputStream(testFile));

        // Act
        documentService.ingestFiles(jobId, files, 1);

        // Assert
        verify(jobService).updateProgress(jobId);
        verify(jobService).markCompleted(jobId);
        verify(jobService).markCompleted(jobId);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(ingestor).ingest(documentCaptor.capture());
        Document ingestedDoc = documentCaptor.getValue();
        assertEquals("1", ingestedDoc.metadata().getString("userId"));

        verify(fileStorageService).delete(testFile.toString());
        verify(fileStorageService).delete(testFile.toString());
    }

    @Test
    void ingestFiles_EmptyList_CompletesWithoutProcessing() throws Exception {
        // Arrange
        List<Path> files = List.of();
        String jobId = "test-job-empty";

        // Act
        documentService.ingestFiles(jobId, files, 1);

        // Assert
        verify(jobService).markCompleted(jobId);
        verify(jobService, never()).updateProgress(any());
        verify(ingestor, never()).ingest(any(Document.class));
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void ingestFiles_ProcessingError_AddsErrorAndContinues() throws Exception {
        // Arrange
        List<Path> files = List.of(testFile);
        String jobId = "test-job-error";

        when(fileStorageService.load(anyString()))
                .thenReturn(Files.newInputStream(testFile));
        doThrow(new RuntimeException("Processing failed"))
                .when(ingestor).ingest(any(Document.class));

        // Act
        documentService.ingestFiles(jobId, files, 1);

        // Assert
        verify(jobService).addError(eq(jobId), anyString());
        verify(jobService, never()).updateProgress(jobId);
        verify(jobService).markCompleted(jobId);
        verify(fileStorageService).delete(testFile.toString());
    }

    @Test
    void ingestFiles_DeletesTempFilesAfterProcessing() throws Exception {
        // Arrange
        List<Path> files = List.of(testFile);
        String jobId = "test-job-cleanup";

        when(fileStorageService.load(anyString()))
                .thenReturn(Files.newInputStream(testFile));

        // Act
        documentService.ingestFiles(jobId, files, 1);

        // Assert
        verify(fileStorageService).delete(testFile.toString());
        verify(ingestor).ingest(any(Document.class));
        verify(jobService).updateProgress(jobId);
        verify(jobService).markCompleted(jobId);
    }

    @Test
    void ingestFiles_NonExistentFile_HandlesGracefully() throws Exception {
        // Arrange
        Path nonExistentFile = tempDir.resolve("non-existent.txt");
        List<Path> files = List.of(nonExistentFile);
        String jobId = "test-job-missing";

        doThrow(new RuntimeException("File not found"))
                .when(fileStorageService).load(nonExistentFile.toString());

        // Act
        documentService.ingestFiles(jobId, files, 1);

        // Assert
        verify(jobService).addError(eq(jobId), anyString());
        verify(jobService, never()).updateProgress(any());
        verify(jobService).markCompleted(jobId);
        verify(fileStorageService).delete(nonExistentFile.toString());
    }

    @Test
    void updateDocument_EmptySummaryDocument_UpdatesSuccessfully() {
        // Arrange
        Integer docId = 1;
        Integer userId = 1;
        com.example.aichatbot.model.Document document = new com.example.aichatbot.model.Document();
        document.setId(docId);
        document.setFilename("old.txt");
        document.setUserId(userId);
        document.setSummary(null);

        UpdateDocumentRequest request = UpdateDocumentRequest.builder()
                .filename("new.txt")
                .summary("New summary")
                .build();

        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = documentService.updateDocument(docId, userId, request);

        // Assert
        assertEquals("new.txt", result.getFilename());
        assertEquals("New summary", result.getSummary());
        verify(documentRepository).save(document);
    }

    @Test
    @Disabled("Requires complex Qdrant mocking")
    void updateDocument_EmptySummaryDocument_RegeneratesSummary() throws Exception {
        // Arrange
        Integer docId = 1;
        Integer userId = 1;
        com.example.aichatbot.model.Document document = new com.example.aichatbot.model.Document();
        document.setId(docId);
        document.setFilename("test.txt");
        document.setUserId(userId);
        document.setSummary(""); // Empty summary

        UpdateDocumentRequest request = UpdateDocumentRequest.builder()
                .filename(null) // excessive null to be sure
                .summary(null)
                .build();

        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(fileStorageService.load("test.txt")).thenReturn(Files.newInputStream(testFile));

        // Mock Qdrant response
        io.qdrant.client.grpc.JsonWithInt.Value textValue = io.qdrant.client.grpc.JsonWithInt.Value.newBuilder()
                .setStringValue("Extracted text from Qdrant")
                .build();

        io.qdrant.client.grpc.Points.RetrievedPoint point = io.qdrant.client.grpc.Points.RetrievedPoint
                .newBuilder()
                .putPayload("text_segment", textValue)
                .build();

        io.qdrant.client.grpc.Points.ScrollResponse scrollResponse = io.qdrant.client.grpc.Points.ScrollResponse
                .newBuilder()
                .addResult(point)
                .build();

        com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.ScrollResponse> future = com.google.common.util.concurrent.Futures
                .immediateFuture(scrollResponse);

        when(qdrantClient.scrollAsync(any(io.qdrant.client.grpc.Points.ScrollPoints.class))).thenReturn(future);

        when(chatModel.chat(anyString())).thenReturn("Regenerated summary");

        // Act
        var result = documentService.updateDocument(docId, userId, request);

        // Assert
        assertEquals("Regenerated summary", result.getSummary());
        verify(chatModel).chat(anyString());
    }

    @Test
    void updateDocument_ExistingSummary_ThrowsException() {
        // Arrange
        Integer docId = 1;
        Integer userId = 1;
        com.example.aichatbot.model.Document document = new com.example.aichatbot.model.Document();
        document.setId(docId);
        document.setSummary("Existing summary");

        UpdateDocumentRequest request = UpdateDocumentRequest.builder().build();

        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.of(document));

        // Act & Assert
        assertThrows(DocumentUpdateException.class,
                () -> documentService.updateDocument(docId, userId, request));
    }

    @Test
    void updateDocument_DocumentNotFound_ThrowsException() {
        // Arrange
        UpdateDocumentRequest request = UpdateDocumentRequest.builder().build();
        when(documentRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> documentService.updateDocument(1, 1, request));
    }
}