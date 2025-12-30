package com.example.aichatbot.service.storage;

import com.example.aichatbot.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageConfig config;

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        if (filename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename: path traversal not allowed");
        }

        // Normalize the path to remove . and .. sequences
        String normalized = Paths.get(filename).normalize().toString();

        // Get just the filename without path to prevent directory traversal
        String name = FilenameUtils.getName(normalized);

        // Validate filename length
        if (name.length() > config.getMaxFilenameLength()) {
            throw new IllegalArgumentException("Filename is too long");
        }

        // Validate file extension
        String extension = FilenameUtils.getExtension(name).toLowerCase();
        if (!config.getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("File type not allowed");
        }

        return name;
    }

    @Override
    public String store(InputStream inputStream, String originalFilename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        try {
            // Sanitize and validate the filename
            String safeFilename = sanitizeFilename(originalFilename);

            // Create the target directory if it doesn't exist
            Path uploadDir = Paths.get(config.getStoragePath()).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate a unique filename with a random prefix
            String uniqueFilename = UUID.randomUUID() + "_" + safeFilename;
            Path targetPath = uploadDir.resolve(uniqueFilename).normalize();

            // Double check that the target path is within the intended directory
            if (!targetPath.startsWith(uploadDir)) {
                throw new SecurityException("Invalid file path");
            }

            // Copy the file to the target location
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {}", targetPath);

            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        } catch (InvalidPathException | SecurityException e) {
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }

    @Override
    public InputStream load(String filename) {
        try {
            Path uploadDir = Paths.get(config.getStoragePath()).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(filename).normalize();

            // Verify that the file is within the upload directory
            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("Access to file is not allowed");
            }

            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file " + filename, e);
        } catch (InvalidPathException | SecurityException e) {
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path uploadDir = Paths.get(config.getStoragePath()).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(filename).normalize();

            // Verify that the file is within the upload directory
            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("Access to file is not allowed");
            }

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file {}", filename, e);
        } catch (InvalidPathException | SecurityException e) {
            log.warn("Invalid file path or access denied: {}", filename, e);
        }
    }

    @Override
    public Path resolve(String filename) {
        try {
            Path uploadDir = Paths.get(config.getStoragePath()).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(filename).normalize();

            // Verify that the file is within the upload directory
            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("Access to file is not allowed");
            }

            return filePath;
        } catch (InvalidPathException | SecurityException e) {
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }
}
