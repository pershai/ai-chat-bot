package com.example.aichatbot.service;

import com.example.aichatbot.exception.ResourceNotFoundException;
import com.example.aichatbot.model.Conversation;
import com.example.aichatbot.model.Message;
import com.example.aichatbot.repository.ConversationRepository;
import com.example.aichatbot.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void createConversation_ValidData_ReturnsConversation() {
        // Arrange
        Conversation newConversation = new Conversation();
        newConversation.setId(1);
        newConversation.setUserId(10);
        newConversation.setTitle("New Chat");

        when(conversationRepository.save(any(Conversation.class))).thenReturn(newConversation);

        // Act
        Conversation result = conversationService.createConversation(10, "New Chat");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(10, result.getUserId());
        assertEquals("New Chat", result.getTitle());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void getUserConversations_ReturnsUserConversations() {
        // Arrange
        Conversation conv1 = new Conversation();
        conv1.setId(1);
        conv1.setUserId(10);

        Conversation conv2 = new Conversation();
        conv2.setId(2);
        conv2.setUserId(10);

        List<Conversation> expectedConversations = Arrays.asList(conv1, conv2);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(10))
                .thenReturn(expectedConversations);

        // Act
        List<Conversation> result = conversationService.getUserConversations(10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(conversationRepository).findByUserIdOrderByUpdatedAtDesc(10);
    }

    @Test
    void getConversation_ExistingId_ReturnsConversation() {
        // Arrange
        Conversation conversation = new Conversation();
        conversation.setId(100);
        when(conversationRepository.findById(100)).thenReturn(Optional.of(conversation));

        // Act
        Optional<Conversation> result = conversationService.getConversation(100);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(100, result.get().getId());
        verify(conversationRepository).findById(100);
    }

    @Test
    void getConversation_NonExistingId_ReturnsEmpty() {
        // Arrange
        when(conversationRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        Optional<Conversation> result = conversationService.getConversation(999);

        // Assert
        assertFalse(result.isPresent());
        verify(conversationRepository).findById(999);
    }

    @Test
    void addMessage_ValidConversation_ReturnsMessage() {
        // Arrange
        Conversation conversation = new Conversation();
        conversation.setId(100);
        conversation.setUpdatedAt(LocalDateTime.now());

        Message savedMessage = new Message();
        savedMessage.setId(1);
        savedMessage.setRole("user");
        savedMessage.setContent("Hello");
        savedMessage.setConversation(conversation);

        when(conversationRepository.findById(100)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // Act
        Message result = conversationService.addMessage(100, "user", "Hello");

        // Assert
        assertNotNull(result);
        assertEquals("user", result.getRole());
        assertEquals("Hello", result.getContent());
        verify(conversationRepository).findById(100);
        verify(conversationRepository).save(conversation);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void addMessage_NonExistingConversation_ThrowsException() {
        // Arrange
        when(conversationRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> conversationService.addMessage(999, "user", "Hello"));

        assertTrue(exception.getMessage().contains("Conversation not found"));
        verify(conversationRepository).findById(999);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getConversationMessages_ReturnsMessages() {
        // Arrange
        Message msg1 = new Message();
        msg1.setId(1);
        msg1.setContent("Hello");

        Message msg2 = new Message();
        msg2.setId(2);
        msg2.setContent("Hi");

        List<Message> expectedMessages = Arrays.asList(msg1, msg2);
        when(messageRepository.findByConversationId(100))
                .thenReturn(expectedMessages);

        // Act
        List<Message> result = conversationService.getConversationMessages(100);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(messageRepository).findByConversationId(100);
    }

    @Test
    void deleteConversation_CallsRepository() {
        // Act
        conversationService.deleteConversation(100);

        // Assert
        verify(conversationRepository).deleteById(100);
    }
}
