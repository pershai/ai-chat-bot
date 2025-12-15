package com.example.aichatbot.service;

import com.example.aichatbot.model.IngestionJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private RedisTemplate<String, IngestionJob> redisTemplate;

    @Mock
    private ValueOperations<String, IngestionJob> valueOperations;

    @InjectMocks
    private JobService jobService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jobService, "completedJobTtlMinutes", 30L);
        ReflectionTestUtils.setField(jobService, "activeJobTtlMinutes", 120L);
    }

    @Test
    void createJob_ValidData_ReturnsJobAndSavesToRedis() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        IngestionJob job = jobService.createJob(5);

        // Assert
        assertNotNull(job);
        assertNotNull(job.getJobId());
        assertEquals(5, job.getTotalFiles());
        assertEquals(IngestionJob.JobStatus.PENDING, job.getStatus());
        assertNotNull(job.getStartTime());
        assertEquals(0, job.getProcessedFiles());

        // Verify save with correct TTL (approx 120 minutes)
        verify(valueOperations).set(eq(job.getJobId()), eq(job), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getJob_ExistingId_ReturnsJob() {
        // Arrange
        String jobId = "test-job-id";
        IngestionJob mockJob = new IngestionJob();
        mockJob.setJobId(jobId);
        mockJob.setTotalFiles(3);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(jobId)).thenReturn(mockJob);

        // Act
        IngestionJob retrievedJob = jobService.getJob(jobId);

        // Assert
        assertNotNull(retrievedJob);
        assertEquals(jobId, retrievedJob.getJobId());
        assertEquals(3, retrievedJob.getTotalFiles());
    }

    @Test
    void getJob_NonExistingId_ReturnsNull() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("non-existent-id")).thenReturn(null);

        // Act
        IngestionJob job = jobService.getJob("non-existent-id");

        // Assert
        assertNull(job);
    }

    @Test
    void updateProgress_ValidJob_UpdatesStatusAndCounter() {
        // Arrange
        String jobId = "test-job-id";
        IngestionJob mockJob = new IngestionJob();
        mockJob.setJobId(jobId);
        mockJob.setStatus(IngestionJob.JobStatus.PENDING);
        mockJob.setProcessedFiles(0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // We need to return the job when get is called
        when(valueOperations.get(jobId)).thenReturn(mockJob);

        // Act
        jobService.updateProgress(jobId);

        // Assert
        assertEquals(IngestionJob.JobStatus.PROCESSING, mockJob.getStatus());
        assertEquals(1, mockJob.getProcessedFiles());

        // Verify Redis updated with refreshed TTL
        verify(valueOperations).set(eq(jobId), eq(mockJob), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    void updateProgress_NonExistingJob_DoesNotThrowException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> jobService.updateProgress("non-existent"));
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void markCompleted_ValidJob_SetsStatusAndEndTime() {
        // Arrange
        String jobId = "test-job-id";
        IngestionJob mockJob = new IngestionJob();
        mockJob.setJobId(jobId);
        mockJob.setStatus(IngestionJob.JobStatus.PROCESSING);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(jobId)).thenReturn(mockJob);

        // Act
        jobService.markCompleted(jobId);

        // Assert
        assertEquals(IngestionJob.JobStatus.COMPLETED, mockJob.getStatus());
        assertNotNull(mockJob.getEndTime());

        verify(valueOperations).set(eq(jobId), eq(mockJob), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void markCompleted_NonExistingJob_DoesNotThrowException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> jobService.markCompleted("non-existent"));
    }

    @Test
    void addError_ValidJob_AddsErrorMessage() {
        // Arrange
        String jobId = "test-job-id";
        IngestionJob mockJob = new IngestionJob();
        mockJob.setJobId(jobId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(jobId)).thenReturn(mockJob);

        // Act
        jobService.addError(jobId, "Error processing file1.pdf");

        // Assert
        assertEquals(1, mockJob.getErrors().size());
        assertTrue(mockJob.getErrors().contains("Error processing file1.pdf"));

        verify(valueOperations).set(eq(jobId), eq(mockJob), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    void addError_NonExistingJob_DoesNotThrowException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> jobService.addError("non-existent", "Some error"));
    }
}
