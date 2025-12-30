package com.example.aichatbot.controller;

import com.example.aichatbot.config.TestSecurityConfig;
import com.example.aichatbot.model.Conversation;
import com.example.aichatbot.model.Message;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.security.JwtAuthenticationFilter;
import com.example.aichatbot.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getConversations_ReturnsUserConversations() throws Exception {
        // Arrange
        Principal mockPrincipal = Mockito.mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("testuser");

        User mockUser = new User();
        mockUser.setId("10");
        mockUser.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        Conversation conv1 = new Conversation();
        conv1.setId(1L);
        conv1.setUserId("10");
        conv1.setTitle("Chat 1");
        conv1.setCreatedAt(LocalDateTime.now());

        Conversation conv2 = new Conversation();
        conv2.setId(2L);
        conv2.setUserId("10");
        conv2.setTitle("Chat 2");
        conv2.setCreatedAt(LocalDateTime.now());

        List<Conversation> conversations = Arrays.asList(conv1, conv2);
        when(conversationService.getUserConversations("10")).thenReturn(conversations);

        // Act & Assert
        mockMvc.perform(get("/api/v1/conversations")
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Chat 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Chat 2"));
    }

    @Test
    void getConversation_ExistingId_ReturnsConversation() throws Exception {
        // Arrange
        Conversation conversation = new Conversation();
        conversation.setId(100L);
        conversation.setUserId("10");
        conversation.setTitle("Test Conversation");
        conversation.setCreatedAt(LocalDateTime.now());

        when(conversationService.getConversation(100L)).thenReturn(Optional.of(conversation));

        // Act & Assert
        mockMvc.perform(get("/api/v1/conversations/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Test Conversation"));
    }

    @Test
    void getConversation_NonExistingId_ReturnsNotFound() throws Exception {
        // Arrange
        when(conversationService.getConversation(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/conversations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessages_ReturnsConversationMessages() throws Exception {
        // Arrange
        Message msg1 = new Message();
        msg1.setId(1L);
        msg1.setRole("user");
        msg1.setContent("Hello");
        // msg1.setTimestamp(LocalDateTime.now());

        Message msg2 = new Message();
        msg2.setId(2L);
        msg2.setRole("assistant");
        msg2.setContent("Hi there!");
        // msg2.setTimestamp(LocalDateTime.now());

        List<Message> messages = Arrays.asList(msg1, msg2);
        when(conversationService.getConversationMessages(100L)).thenReturn(messages);

        // Act & Assert
        mockMvc.perform(get("/api/v1/conversations/100/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("assistant"))
                .andExpect(jsonPath("$[0].content").value("Hi there!"))
                .andExpect(jsonPath("$[1].role").value("user"))
                .andExpect(jsonPath("$[1].content").value("Hello"));
    }

    @Test
    void deleteConversation_CallsServiceAndReturnsNoContent() throws Exception {
        // Arrange
        Long conversationId = 123L;
        doNothing().when(conversationService).deleteConversation(conversationId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/conversations/{id}", conversationId))
                .andExpect(status().isNoContent());

        verify(conversationService).deleteConversation(conversationId);
    }
}
