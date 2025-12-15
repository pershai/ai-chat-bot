package com.example.aichatbot.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceTest {

    private LocalFileStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageService();
        ReflectionTestUtils.setField(service, "storagePath", tempDir.resolve("ingest").toString());
    }

    @Test
    void store_ValidFile_StoresFileAndReturnsPath() throws Exception {
        // Arrange
        String filename = "test.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // Act
        String storedPathString = service.store(inputStream, filename);

        // Assert
        Path storedPath = Path.of(storedPathString);
        assertTrue(Files.exists(storedPath), "Stored file should exist");
        assertEquals(content, Files.readString(storedPath), "Files content should match");
        assertTrue(storedPath.toString().endsWith("_" + filename), "Filename should contain original name");
    }

    @Test
    void load_ExistingFile_ReturnsInputStream() throws Exception {
        // Arrange
        String filename = "test-load.txt";
        String content = "Load me!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String storedPath = service.store(inputStream, filename);

        // Act
        try (InputStream loadedStream = service.load(storedPath)) {
            // Assert
            String loadedContent = StreamUtils.copyToString(loadedStream, StandardCharsets.UTF_8);
            assertEquals(content, loadedContent, "Loaded content should match stored content");
        }
    }

    @Test
    void delete_ExistingFile_DeletesFile() {
        // Arrange
        String filename = "test-delete.txt";
        String storedPath = service.store(new ByteArrayInputStream(new byte[0]), filename);

        // Act
        service.delete(storedPath);

        // Assert
        assertFalse(Files.exists(Path.of(storedPath)), "File should be deleted");
    }

    @Test
    void resolve_ReturnsPathObject() {
        // Arrange
        String pathString = tempDir.resolve("some/path").toString();

        // Act
        Path resolved = service.resolve(pathString);

        // Assert
        assertEquals(Path.of(pathString), resolved);
    }

}
