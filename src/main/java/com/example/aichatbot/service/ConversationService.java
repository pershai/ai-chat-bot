package com.example.aichatbot.service;

import com.example.aichatbot.exception.ResourceNotFoundException;
import com.example.aichatbot.model.Conversation;
import com.example.aichatbot.model.Message;
import com.example.aichatbot.repository.ConversationRepository;
import com.example.aichatbot.repository.MessageRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public Conversation createConversation(String userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getUserConversations(String userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Optional<Conversation> getConversation(Long id) {
        return conversationRepository.findById(id);
    }

    @CacheEvict(value = "messages", key = "#conversationId")
    public Message addMessage(Long conversationId, String role, String content) {
        return addMessage(conversationId, role, content, 0, 0);
    }

    @CacheEvict(value = "messages", key = "#conversationId")
    public Message addMessage(Long conversationId, String role, String content, int inputTokens, int outputTokens) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setInputTokens(inputTokens);
        message.setOutputTokens(outputTokens);

        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        return messageRepository.save(message);
    }

    @Cacheable(value = "messages", key = "#conversationId")
    public List<Message> getConversationMessages(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    @CacheEvict(value = "messages", key = "#id")
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }
}
