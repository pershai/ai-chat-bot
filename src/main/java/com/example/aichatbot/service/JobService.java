package com.example.aichatbot.service;

import com.example.aichatbot.model.IngestionJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final RedisTemplate<String, IngestionJob> redisTemplate;

    @Value("${app.job.ttl.active-minutes}")
    private long activeJobTtlMinutes;

    @Value("${app.job.ttl.completed-minutes}")
    private long completedJobTtlMinutes;

    public IngestionJob createJob(int totalFiles) {
        String id = UUID.randomUUID().toString();
        IngestionJob job = new IngestionJob();
        job.setJobId(id);
        job.setStatus(IngestionJob.JobStatus.PENDING);
        job.setTotalFiles(totalFiles);
        job.setStartTime(LocalDateTime.now());

        saveJob(job, activeJobTtlMinutes);
        return job;
    }

    public IngestionJob getJob(String jobId) {
        return redisTemplate.opsForValue().get(jobId);
    }

    public void updateProgress(String jobId) {
        IngestionJob job = getJob(jobId);
        if (job != null) {
            job.setStatus(IngestionJob.JobStatus.PROCESSING);
            job.setProcessedFiles(job.getProcessedFiles() + 1);
            // Refresh TTL on activity
            saveJob(job, activeJobTtlMinutes);
        }
    }

    public void markCompleted(String jobId) {
        IngestionJob job = getJob(jobId);
        if (job != null) {
            job.setStatus(IngestionJob.JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            // Shorten TTL for completed jobs
            saveJob(job, completedJobTtlMinutes);
        }
    }

    public void addError(String jobId, String errorMsg) {
        IngestionJob job = getJob(jobId);
        if (job != null) {
            job.getErrors().add(errorMsg);
            saveJob(job, activeJobTtlMinutes);
        }
    }

    private void saveJob(IngestionJob job, long ttlMinutes) {
        redisTemplate.opsForValue().set(job.getJobId(), job, ttlMinutes, TimeUnit.MINUTES);
    }
}