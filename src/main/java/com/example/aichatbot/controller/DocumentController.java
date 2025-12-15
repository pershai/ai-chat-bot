package com.example.aichatbot.controller;

import com.example.aichatbot.dto.DocumentDto;
import com.example.aichatbot.model.IngestionEvent;
import com.example.aichatbot.model.IngestionJob;
import com.example.aichatbot.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestDocs(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "userId", required = false) Integer userId) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Integer effectiveUserId = userId != null ? userId : 1;
        IngestionJob job = jobService.createJob(files.size());
        List<String> validPaths = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String path = fileStorageService.store(file.getInputStream(), file.getOriginalFilename());
                validPaths.add(path);
            }

            // Publish Event
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
    public ResponseEntity<List<DocumentDto>> getDocuments(
            @RequestParam(value = "userId", required = false) Integer userId) {
        Integer effectiveUserId = userId != null ? userId : 1;
        return ResponseEntity.ok(documentService.getDocuments(effectiveUserId));
    }
}