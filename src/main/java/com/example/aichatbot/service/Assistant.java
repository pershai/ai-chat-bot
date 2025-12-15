package com.example.aichatbot.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import dev.langchain4j.service.Result;

public interface Assistant {

    @SystemMessage("{{systemPrompt}}")
    Result<String> chat(@MemoryId String conversationId, @V("systemPrompt") String systemPrompt,
            @UserMessage String userMessage);
}