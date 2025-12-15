package com.example.aichatbot.controller;

import com.example.aichatbot.model.Conversation;
import com.example.aichatbot.model.Message;
import com.example.aichatbot.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<Conversation>> getConversations(@RequestParam Integer userId) {
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable Integer id) {
        return conversationService.getConversation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Integer id) {
        List<Message> conversationMessages = conversationService.getConversationMessages(id);
        if (conversationMessages.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        conversationMessages.sort(Comparator.comparing(Message::getId).reversed());
        return ResponseEntity.ok(conversationMessages);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Integer id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
