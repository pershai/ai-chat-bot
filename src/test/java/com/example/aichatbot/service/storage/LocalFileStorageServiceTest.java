package com.example.aichatbot.service.storage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.example.aichatbot.config.FileStorageConfig;
import org.apache.tika.Tika;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    @Mock
    private FileStorageConfig fileStorageConfig;

    @Mock
    private Tika tika;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<ILoggingEvent> logCaptor;

    private LocalFileStorageService service;

    @TempDir
    Path tempDir;

    private void setupService() {
        service = new LocalFileStorageService(fileStorageConfig, tika);
    }

    private void setupLogging() {
        Logger logger = (Logger) LoggerFactory.getLogger(LocalFileStorageService.class);
        logger.addAppender(mockAppender);
        logger.setLevel(Level.WARN);
        logger.setAdditive(true);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(LocalFileStorageService.class);
        logger.detachAppender(mockAppender);
    }

    private void setupBasicMocks() {
        lenient().when(fileStorageConfig.getStoragePath()).thenReturn(tempDir.toString());
        setupService();
    }

    private void setupWithAllowedExtensions(String... extensions) {
        lenient().when(fileStorageConfig.getStoragePath()).thenReturn(tempDir.toString());
        lenient().when(fileStorageConfig.getAllowedExtensions()).thenReturn(List.of(extensions));
        lenient().when(fileStorageConfig.getMaxFilenameLength()).thenReturn(255);
        setupService();
    }

    @Test
    void store_ValidFile_StoresFileAndReturnsPath() throws Exception {
        // Arrange
        setupWithAllowedExtensions("txt");
        String filename = "test.txt";
        String content = "Hello, World!";
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        when(tika.detect(any(InputStream.class))).thenReturn("text/plain");

        // Act
        String result = service.store(input, filename);

        // Assert
        assertNotNull(result);
        Path storedFile = Path.of(result);
        assertTrue(Files.exists(storedFile));
        assertEquals(content, Files.readString(storedFile));
    }

    @Test
    void store_WithEmptyFilename_ThrowsException() {
        // Arrange
        setupBasicMocks();
        InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.store(input, ""));
    }

    @Test
    void store_WithNullInput_ThrowsException() {
        // Arrange
        setupBasicMocks();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.store(null, "test.txt"));
    }

    @Test
    void load_ExistingFile_ReturnsContent() throws Exception {
        // Arrange
        setupBasicMocks();
        String filename = "test.txt";
        String content = "Test content";
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

        // Act
        try (InputStream inputStream = service.load(filePath.toString())) {
            // Assert
            assertNotNull(inputStream);
            String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(content, result);
        }
    }

    @Test
    void load_NonExistentFile_ThrowsException() {
        // Arrange
        setupBasicMocks();
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> service.load(nonExistentFile.toString()));
    }

    @Test
    void load_OutsideStorageDirectory_ThrowsException() throws Exception {
        // Arrange
        setupBasicMocks();
        Path outsideFile = tempDir.getParent().resolve("outside.txt");
        Files.write(outsideFile, "test".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.load(outsideFile.toString()));

        // Cleanup
        Files.deleteIfExists(outsideFile);
    }

    @Test
    void delete_ExistingFile_DeletesFile() throws Exception {
        // Arrange
        setupBasicMocks();
        String filename = "test.txt";
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, "test".getBytes(StandardCharsets.UTF_8));

        // Act
        service.delete(filePath.toString());

        // Assert
        assertFalse(Files.exists(filePath));
    }

    @Test
    void delete_NonExistentFile_DoesNotThrow() {
        // Arrange
        setupBasicMocks();
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // Act & Assert
        assertDoesNotThrow(() -> service.delete(nonExistentFile.toString()));
    }

    @Test
    void delete_OutsideStorageDirectory_LogsWarning() throws Exception {
        // Arrange
        when(fileStorageConfig.getStoragePath()).thenReturn(tempDir.resolve("ingest").toString());
        setupService();
        setupLogging();

        Path outsideFile = tempDir.getParent().resolve("outside.txt");
        Files.write(outsideFile, "test".getBytes(StandardCharsets.UTF_8));

        // Act
        service.delete(outsideFile.toString());

        // Verify the warning was logged
        verify(mockAppender).doAppend(logCaptor.capture());
        List<ILoggingEvent> logs = logCaptor.getAllValues().stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();

        assertFalse(logs.isEmpty(), "Expected warning log not found");
        assertTrue(logs.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("Invalid file path or access denied")),
                "Expected warning message not found in logs");

        // Cleanup
        Files.deleteIfExists(outsideFile);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test.pdf", "test.doc", "test.docx", "test.txt", "test.md",
            "test.csv", "test.json", "test.xls", "test.xlsx", "test.ppt", "test.pptx"
    })
    void store_WithValidExtensions_StoresSuccessfully(String filename) throws Exception {
        // Arrange
        setupWithAllowedExtensions("pdf", "doc", "docx", "txt", "md", "csv", "json", "xls", "xlsx", "ppt", "pptx");

        InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        String extension = filename.substring(filename.lastIndexOf(".") + 1);
        String mimeType = getMimeTypeForExtension(extension);

        when(tika.detect(any(InputStream.class))).thenReturn(mimeType);

        // Act & Assert
        assertDoesNotThrow(() -> {
            String result = service.store(input, filename);
            assertNotNull(result);
            assertTrue(Files.exists(Path.of(result)));
            Files.deleteIfExists(Path.of(result));
        });
    }

    private String getMimeTypeForExtension(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt", "md", "csv", "json" -> "text/plain";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }

    @Test
    void store_WithInvalidExtension_ThrowsException() {
        // Arrange
        setupWithAllowedExtensions("txt");
        String filename = "test.invalid";
        InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.store(input, filename));
    }

    @Test
    void store_WithPathTraversal_ThrowsException() {
        // Arrange
        setupWithAllowedExtensions("txt");
        String maliciousFilename = "../malicious.txt";
        InputStream input = new ByteArrayInputStream("malicious".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.store(input, maliciousFilename));
    }

    @Test
    void resolve_ValidPath_ReturnsPath() {
        // Arrange
        setupBasicMocks();
        String filename = "test.txt";
        Path expectedPath = tempDir.resolve(filename).normalize();

        // Act
        Path result = service.resolve(filename);

        // Assert
        assertEquals(expectedPath, result);
    }

    @Test
    void resolve_OutsideStorageDirectory_ThrowsException() {
        // Arrange
        setupBasicMocks();
        String maliciousPath = "../outside.txt";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.resolve(maliciousPath));
    }
}