package com.example.aichatbot.controller;

import com.example.aichatbot.dto.ChatRequestDto;
import com.example.aichatbot.model.Conversation;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.security.JwtAuthenticationFilter;
import com.example.aichatbot.service.ChatService;
import com.example.aichatbot.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJson
@Import({JacksonAutoConfiguration.class, TestSecurityConfig.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Mock Security Context
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getName()).thenReturn("testuser");
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock User Lookup
        User mockUser = new com.example.aichatbot.model.User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(mockUser));
    }

    @Test
    void chat_ExistingConversation_ReturnsResponse() throws Exception {
        // Arrange
        ChatRequestDto request = new ChatRequestDto(1, 100, "Hello", null);

        when(chatService.processChat(anyInt(), anyInt(), anyString(), any())).thenReturn("AI Response");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("AI Response"));
    }

    @Test
    void chat_NewConversation_CreatesAndReturnsResponse() throws Exception {
        // Arrange
        ChatRequestDto request = new ChatRequestDto(1, null, "New Chat", null);

        Conversation mockConv = new Conversation();
        mockConv.setId(200);

        when(conversationService.createConversation(eq(1), anyString())).thenReturn(mockConv);
        when(chatService.processChat(eq(1), eq(200), anyString(), any())).thenReturn("Welcome");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Welcome"));
    }

    private static <T> T eq(T value) {
        return ArgumentMatchers.eq(value);
    }

    @TestConfiguration
    static class TestObjectMapperConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
