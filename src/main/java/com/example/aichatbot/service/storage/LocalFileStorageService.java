package com.example.aichatbot.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.ingestion.storage-path:./data/ingest}")
    private String storagePath;

    @Override
    public String store(InputStream inputStream, String originalFilename) {
        try {
            Path ingestDir = Paths.get(storagePath);
            if (!Files.exists(ingestDir)) {
                Files.createDirectories(ingestDir);
            }
            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = ingestDir.resolve(uniqueFilename).toAbsolutePath();
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public InputStream load(String filename) {
        try {
            Path filePath = Paths.get(filename);
            if (!Files.exists(filePath)) {
                filePath = Paths.get(storagePath, Paths.get(filename).getFileName().toString());
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Files.deleteIfExists(Paths.get(filename));
        } catch (IOException e) {
            log.warn("Failed to delete file {}", filename, e);
        }
    }

    @Override
    public Path resolve(String filename) {
        return Paths.get(filename);
    }
}
