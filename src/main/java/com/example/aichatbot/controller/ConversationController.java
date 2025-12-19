package com.example.aichatbot.controller;

import com.example.aichatbot.exception.AuthenticationException;
import com.example.aichatbot.exception.UserNotFoundException;
import com.example.aichatbot.model.Conversation;
import com.example.aichatbot.model.Message;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Conversation>> getConversations(
            Principal principal,
            @RequestParam(required = false) String tenantId) {
        if (principal == null) {
            throw new AuthenticationException("No authentication found");
        }
        String username = principal.getName();
        String userId = userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new UserNotFoundException("User", username));

        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable Long id) {
        return conversationService.getConversation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long id) {
        List<Message> conversationMessages = conversationService.getConversationMessages(id);
        if (conversationMessages.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        conversationMessages.sort(Comparator.comparing(Message::getId).reversed());
        return ResponseEntity.ok(conversationMessages);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
