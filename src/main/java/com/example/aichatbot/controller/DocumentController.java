package com.example.aichatbot.controller;

import com.example.aichatbot.dto.DocumentDto;
import com.example.aichatbot.dto.UpdateDocumentRequest;
import com.example.aichatbot.exception.DocumentUpdateException;
import com.example.aichatbot.exception.UserNotFoundException;
import com.example.aichatbot.model.IngestionEvent;
import com.example.aichatbot.model.IngestionJob;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final JobService jobService;
    private final com.example.aichatbot.service.messaging.IngestionProducer ingestionProducer;
    private final com.example.aichatbot.service.storage.FileStorageService fileStorageService;
    private final com.example.aichatbot.service.DocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestDocs(
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Integer effectiveUserId = getAuthenticatedUserId();
        IngestionJob job = jobService.createJob(files.size());
        List<String> validPaths = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String path = fileStorageService.store(file.getInputStream(), file.getOriginalFilename());
                validPaths.add(path);
            }

            IngestionEvent event = new IngestionEvent(
                    job.getJobId(), effectiveUserId, validPaths);

            ingestionProducer.publish(event);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "Processing queued",
                    "jobId", job.getJobId()));

        } catch (IOException e) {
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
    public ResponseEntity<List<DocumentDto>> getDocuments() {
        Integer effectiveUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(documentService.getDocuments(effectiveUserId));
    }

    private Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UserNotFoundException("No authentication found", "unknown");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(com.example.aichatbot.model.User::getId)
                .orElseThrow(() -> new UserNotFoundException("User", username));
    }

    @PatchMapping("/{documentId}")
    public ResponseEntity<DocumentDto> updateDocument(
            @PathVariable Integer documentId,
            @RequestBody(required = false) @Valid UpdateDocumentRequest request) {
        if (request == null) {
            request = new UpdateDocumentRequest();
        }
        try {
            Integer userId = getAuthenticatedUserId();
            DocumentDto documentDto = documentService.updateDocument(documentId, userId, request);
            return ResponseEntity.ok(documentDto);
        } catch (IllegalStateException e) {
            throw new DocumentUpdateException(e.getMessage());
        }
    }
}