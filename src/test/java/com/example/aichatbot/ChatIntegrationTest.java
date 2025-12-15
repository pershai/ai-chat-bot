package com.example.aichatbot;

import com.example.aichatbot.dto.ChatRequestDto;
import com.example.aichatbot.dto.ChatResponseDto;
import com.example.aichatbot.dto.LoginRequestDto;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public class ChatIntegrationTest extends AbstractIntegrationTest {

        @Container
        private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
                        .withDatabaseName("testdb")
                        .withUsername("test")
                        .withPassword("test");

        @Container
        private static final GenericContainer<?> qdrant = new GenericContainer<>("qdrant/qdrant:latest")
                        .withExposedPorts(6333, 6334);

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);

                registry.add("qdrant.host", qdrant::getHost);
                registry.add("qdrant.grpc.port", () -> qdrant.getMappedPort(6334));
        }

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private UserRepository userRepository;

        @MockitoBean
        private com.example.aichatbot.service.Assistant assistant;

        @Test
        void testFullChatFlow() {
                org.mockito.Mockito
                                .when(assistant.chat(org.mockito.ArgumentMatchers.anyString(),
                                                org.mockito.ArgumentMatchers.anyString(),
                                                org.mockito.ArgumentMatchers.anyString()))
                                .thenReturn(dev.langchain4j.service.Result.<String>builder()
                                                .content("I am a test bot")
                                                .tokenUsage(new dev.langchain4j.model.output.TokenUsage(10, 10))
                                                .finishReason(dev.langchain4j.model.output.FinishReason.STOP)
                                                .build());

                // 1. Register
                LoginRequestDto registerRequest = new LoginRequestDto("integrationUser", "password123");

                ResponseEntity<User> registerResponse = restTemplate.postForEntity("/auth/register", registerRequest,
                                User.class);
                assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(userRepository.findByUsername("integrationUser")).isPresent();

                // 2. Login
                LoginRequestDto loginRequestDto = new LoginRequestDto("integrationUser", "password123");

                ResponseEntity<ChatResponseDto> loginResponse = restTemplate.postForEntity("/auth/login",
                                loginRequestDto,
                                ChatResponseDto.class);
                assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(loginResponse.getBody()).isNotNull();
                String token = loginResponse.getBody().response();
                assertThat(token).isNotEmpty();

                // 3. Chat
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);

                ChatRequestDto chatRequestDto = new ChatRequestDto(1, 1, "Hello Integration Test", null);

                HttpEntity<ChatRequestDto> requestEntity = new HttpEntity<>(chatRequestDto, headers);

                ResponseEntity<ChatResponseDto> chatResponse = restTemplate.exchange(
                                "/api/v1/chat",
                                HttpMethod.POST,
                                requestEntity,
                                ChatResponseDto.class);

                assertThat(chatResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(chatResponse.getBody()).isNotNull();
                assertThat(chatResponse.getBody().response()).isEqualTo("I am a test bot");
        }
}
