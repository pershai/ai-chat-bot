package com.example.aichatbot.controller;

import com.example.aichatbot.config.TestSecurityConfig;
import com.example.aichatbot.dto.DocumentDto;
import com.example.aichatbot.model.IngestionJob;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.security.JwtAuthenticationFilter;
import com.example.aichatbot.service.DocumentService;
import com.example.aichatbot.service.JobService;
import com.example.aichatbot.service.messaging.IngestionProducer;
import com.example.aichatbot.service.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class DocumentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private JobService jobService;

        @MockitoBean
        private IngestionProducer ingestionProducer;

        @MockitoBean
        private FileStorageService fileStorageService;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private DocumentService documentService;

        @MockitoBean
        private UserRepository userRepository;

        @Test
        void ingestDocs_ValidFiles_ReturnsAccepted() throws Exception {
                // Arrange
                IngestionJob mockJob = new IngestionJob();
                mockJob.setJobId("test-job-123");
                mockJob.setStatus(IngestionJob.JobStatus.PENDING);

                when(jobService.createJob(anyInt())).thenReturn(mockJob);

                MockMultipartFile file1 = new MockMultipartFile(
                                "files",
                                "test1.txt",
                                "text/plain",
                                "Test content 1".getBytes());

                MockMultipartFile file2 = new MockMultipartFile(
                                "files",
                                "test2.pdf",
                                "application/pdf",
                                "PDF content".getBytes());

                User mockUser = new User();
                mockUser.setId("user-123");
                mockUser.setUsername("testuser");
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

                // Act & Assert
                mockMvc.perform(multipart("/api/v1/documents/ingest")
                                .file(file1)
                                .file(file2)
                                .principal(() -> "testuser"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.message").value("Processing queued"))
                                .andExpect(jsonPath("$.jobId").value("test-job-123"));
        }

        @Test
        void ingestDocs_EmptyFileList_ReturnsBadRequest() throws Exception {
                // Act & Assert
                mockMvc.perform(multipart("/api/v1/documents/ingest")
                                .principal(() -> "testuser"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getStatus_ExistingJob_ReturnsJob() throws Exception {
                // Arrange
                IngestionJob job = new IngestionJob();
                job.setJobId("job-456");
                job.setStatus(IngestionJob.JobStatus.PROCESSING);
                job.setTotalFiles(5);
                job.setProcessedFiles(3);

                when(jobService.getJob("job-456")).thenReturn(job);

                // Act & Assert
                mockMvc.perform(get("/api/v1/documents/status/job-456"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.jobId").value("job-456"))
                                .andExpect(jsonPath("$.status").value("PROCESSING"))
                                .andExpect(jsonPath("$.totalFiles").value(5))
                                .andExpect(jsonPath("$.processedFiles").value(3));
        }

        @Test
        void getStatus_NonExistentJob_ReturnsNotFound() throws Exception {
                // Arrange
                when(jobService.getJob("non-existent")).thenReturn(null);

                // Act & Assert
                mockMvc.perform(get("/api/v1/documents/status/non-existent"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void ingestDocs_SingleFile_ProcessesSuccessfully() throws Exception {
                // Arrange
                IngestionJob mockJob = new IngestionJob();
                mockJob.setJobId("single-file-job");

                when(jobService.createJob(1)).thenReturn(mockJob);

                MockMultipartFile file = new MockMultipartFile(
                                "files",
                                "document.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "Document content".getBytes());

                User mockUser = new User();
                mockUser.setId("user-123");
                mockUser.setUsername("testuser");
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

                // Act & Assert
                mockMvc.perform(multipart("/api/v1/documents/ingest")
                                .file(file)
                                .principal(() -> "testuser"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.jobId").value("single-file-job"));
        }

        @Test
        void getDocuments_ReturnsList() throws Exception {
                // Arrange
                DocumentDto doc1 = DocumentDto.builder()
                                .id(1L)
                                .filename("test.pdf")
                                .fileType("PDF")
                                .build();

                when(documentService.getDocuments(anyString())).thenReturn(List.of(doc1));

                User mockUser = new User();
                mockUser.setId("user-123");
                mockUser.setUsername("testuser");
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

                // Act & Assert
                mockMvc.perform(get("/api/v1/documents")
                                .principal(() -> "testuser"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].filename").value("test.pdf"))
                                .andExpect(jsonPath("$[0].fileType").value("PDF"));
        }
}
