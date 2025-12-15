package com.example.aichatbot.service;

import com.example.aichatbot.dto.DocumentDto;
import com.example.aichatbot.repository.DocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final EmbeddingStoreIngestor ingestor;
    private final JobService jobService;
    private final DocumentRepository documentRepository;
    private final com.example.aichatbot.service.storage.FileStorageService fileStorageService;

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
            ingestor.ingest(document);

            com.example.aichatbot.model.Document dbDocument = new com.example.aichatbot.model.Document();
            dbDocument.setFilename(fileStorageService.resolve(filename).getFileName().toString());
            dbDocument.setUserId(userId);
            dbDocument.setFileType(getFileExtension(filename));
            dbDocument.setUploadDate(LocalDateTime.now());
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
                        .build())
                .collect(Collectors.toList());
    }
}
