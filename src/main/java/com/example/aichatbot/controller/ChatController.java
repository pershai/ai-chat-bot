package com.example.aichatbot.controller;

import com.example.aichatbot.dto.ChatRequestDto;
import com.example.aichatbot.dto.ChatResponseDto;
import com.example.aichatbot.exception.AuthenticationException;
import com.example.aichatbot.exception.UserNotFoundException;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.service.ChatService;
import com.example.aichatbot.service.ConversationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Tag(name = "Chat", description = "Chat operations")
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final com.example.aichatbot.repository.UserRepository userRepository;

    public ChatController(ChatService chatService, ConversationService conversationService,
            UserRepository userRepository) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto request,
            Principal principal) {
        if (principal == null) {
            throw new AuthenticationException("No authentication found");
        }
        String username = principal.getName();
        String userId = userRepository.findByUsername(username)
                .map(com.example.aichatbot.model.User::getId)
                .orElseThrow(() -> new UserNotFoundException("User", username));

        Long conversationId = request.conversationId();
        boolean existsAndOwned = false;
        if (conversationId != null) {
            existsAndOwned = conversationService.getConversation(conversationId)
                    .map(conv -> conv.getUserId().equals(userId))
                    .orElse(false);
        }

        if (!existsAndOwned) {
            var conv = conversationService.createConversation(userId, "New Chat");
            conversationId = conv.getId();
        }

        String responseText = chatService.processChat(userId, conversationId, request.message(),
                request.botConfig());

        return ResponseEntity.ok(new ChatResponseDto(responseText, conversationId));
    }
}
