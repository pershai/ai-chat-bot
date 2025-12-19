package com.example.aichatbot.service;

import com.example.aichatbot.repository.DocumentRepository;
import com.example.aichatbot.service.storage.FileStorageService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
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

        com.example.aichatbot.model.Document savedDoc = new com.example.aichatbot.model.Document();
        savedDoc.setId(1L);
        when(documentRepository.save(any(com.example.aichatbot.model.Document.class)))
                .thenReturn(savedDoc);

        when(chatModel.chat(anyString())).thenReturn("Test summary");

        // Act
        documentService.ingestFiles(jobId, files, "1");

        // Assert
        verify(jobService).updateProgress(jobId);
        verify(jobService).markCompleted(jobId);
        verify(ingestor).ingest(any(Document.class));
        verify(fileStorageService).delete(testFile.toString());
    }

    @Test
    void ingestFiles_EmptyList_CompletesWithoutProcessing() throws Exception {
        // Arrange
        List<Path> files = List.of();
        String jobId = "test-job-empty";

        // Act
        documentService.ingestFiles(jobId, files, "1");

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
        documentService.ingestFiles(jobId, files, "1");

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

        // Mock the document repository to return a document with ID when save is called
        com.example.aichatbot.model.Document savedDoc = new com.example.aichatbot.model.Document();
        savedDoc.setId(1L);
        when(documentRepository.save(any(com.example.aichatbot.model.Document.class)))
                .thenReturn(savedDoc);

        // Mock the chat model to return a summary
        when(chatModel.chat(anyString())).thenReturn("Test summary");

        // Act
        documentService.ingestFiles(jobId, files, "1");

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
        documentService.ingestFiles(jobId, files, "1");

        // Assert
        verify(jobService).addError(eq(jobId), anyString());
        verify(jobService, never()).updateProgress(any());
        verify(jobService).markCompleted(jobId);
        verify(fileStorageService).delete(nonExistentFile.toString());
    }
}