package com.example.aichatbot.service.storage;

import com.example.aichatbot.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageConfig config;
    private final Tika tika;

    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Mapping of file extensions to expected MIME types
    private static final Map<String, String[]> EXTENSION_TO_MIME = new HashMap<>();
    static {
        EXTENSION_TO_MIME.put("pdf", new String[] { "application/pdf" });
        EXTENSION_TO_MIME.put("doc", new String[] { "application/msword" });
        EXTENSION_TO_MIME.put("docx",
                new String[] { "application/vnd.openxmlformats-officedocument.wordprocessingml.document" });
        EXTENSION_TO_MIME.put("txt", new String[] { "text/plain" });
        EXTENSION_TO_MIME.put("md", new String[] { "text/plain", "text/markdown" });
        EXTENSION_TO_MIME.put("csv", new String[] { "text/csv", "text/plain" });
        EXTENSION_TO_MIME.put("json", new String[] { "application/json", "text/plain" });
        EXTENSION_TO_MIME.put("xls", new String[] { "application/vnd.ms-excel" });
        EXTENSION_TO_MIME.put("xlsx",
                new String[] { "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
        EXTENSION_TO_MIME.put("ppt", new String[] { "application/vnd.ms-powerpoint" });
        EXTENSION_TO_MIME.put("pptx",
                new String[] { "application/vnd.openxmlformats-officedocument.presentationml.presentation" });
    }

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
            String extension = FilenameUtils.getExtension(safeFilename).toLowerCase();

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

            // Use BufferedInputStream to support mark/reset for content validation
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);

            // Validate file content
            validateFileContent(bufferedStream, extension);

            // Copy the file to the target location
            Files.copy(bufferedStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Validate file size after upload
            long fileSize = Files.size(targetPath);
            if (fileSize == 0) {
                Files.deleteIfExists(targetPath);
                throw new IllegalArgumentException("Empty files are not allowed");
            }
            if (fileSize > MAX_FILE_SIZE) {
                Files.deleteIfExists(targetPath);
                throw new IllegalArgumentException(
                        "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
            }

            log.info("Stored file: {} (size: {} bytes)", targetPath, fileSize);

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

    /**
     * Validates file content by checking MIME type matches the file extension.
     * This prevents malicious files disguised with fake extensions.
     */
    private void validateFileContent(BufferedInputStream inputStream, String extension) throws IOException {
        // Mark the stream so we can reset it after detection
        inputStream.mark(8192); // Mark with a reasonable read-ahead limit

        try {
            // Detect actual MIME type from content
            String detectedMimeType = tika.detect(inputStream);

            // Reset stream for later use
            inputStream.reset();

            // Get expected MIME types for this extension
            String[] expectedMimeTypes = EXTENSION_TO_MIME.get(extension);
            if (expectedMimeTypes == null) {
                log.warn("No MIME type mapping for extension: {}", extension);
                return; // Allow it if we don't have a mapping
            }

            // Check if detected MIME type matches any expected type
            boolean mimeTypeMatches = false;
            for (String expectedMime : expectedMimeTypes) {
                if (detectedMimeType.startsWith(expectedMime)) {
                    mimeTypeMatches = true;
                    break;
                }
            }

            if (!mimeTypeMatches) {
                log.warn("MIME type mismatch - Extension: {}, Detected: {}, Expected: {}",
                        extension, detectedMimeType, String.join(", ", expectedMimeTypes));
                throw new IllegalArgumentException(
                        "File content does not match the file extension. Detected type: " + detectedMimeType);
            }

            log.debug("File validation passed - Extension: {}, MIME type: {}", extension, detectedMimeType);
        } catch (IOException e) {
            log.error("Failed to validate file content", e);
            throw e;
        }
    }
}
