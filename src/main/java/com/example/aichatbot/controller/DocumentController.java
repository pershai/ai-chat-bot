package com.example.aichatbot.controller;

import com.example.aichatbot.dto.DocumentDto;
import com.example.aichatbot.exception.UserNotFoundException;
import com.example.aichatbot.model.IngestionEvent;
import com.example.aichatbot.model.IngestionJob;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.service.DocumentService;
import com.example.aichatbot.service.JobService;
import com.example.aichatbot.service.messaging.IngestionProducer;
import com.example.aichatbot.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final JobService jobService;
    private final IngestionProducer ingestionProducer;
    private final FileStorageService fileStorageService;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestDocs(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Principal principal) {

        if (principal == null) {
            log.error("Ingestion request received but Principal is null. Ensure security is correctly configured.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        log.info("Ingestion request received from user: {}. Number of files: {}",
                principal.getName(), (files != null ? files.size() : 0));

        if (files == null || files.isEmpty()) {
            log.warn("Ingestion request from {} failed: No files provided", principal.getName());
            return ResponseEntity.badRequest().body(Map.of("error",
                    "No files provided for ingestion. Ensure 'files' field is present in multipart/form-data."));
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + principal.getName()));

        String userId = user.getId();
        log.debug("Found user {} with ID {}", user.getUsername(), userId);

        IngestionJob job = jobService.createJob(files.size());
        List<String> validPaths = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("Skipping empty file: {}", file.getOriginalFilename());
                    continue;
                }
                String path = fileStorageService.store(file.getInputStream(), file.getOriginalFilename());
                validPaths.add(path);
                log.debug("Stored file: {} at {}", file.getOriginalFilename(), path);
            }

            if (validPaths.isEmpty()) {
                log.warn("No valid files were stored from the request");
                return ResponseEntity.badRequest().body(Map.of("error", "All provided files were empty"));
            }

            // Publish Event
            IngestionEvent event = new IngestionEvent(job.getJobId(), userId, validPaths);
            ingestionProducer.publish(event);
            log.info("Ingestion job {} queued with {} files for user {}", job.getJobId(), validPaths.size(), userId);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "Processing queued",
                    "jobId", job.getJobId()));

        } catch (IOException e) {
            log.error("Failed to store files for job {}: {}", job.getJobId(), e.getMessage(), e);
            jobService.addError(job.getJobId(), "Failed to save files: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<IngestionJob> getStatus(@PathVariable String jobId) {
        IngestionJob job = jobService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getDocuments(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + principal.getName()));

        return ResponseEntity.ok(documentService.getDocuments(user.getId()));
    }
}