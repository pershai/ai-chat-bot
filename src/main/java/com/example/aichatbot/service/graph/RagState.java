package com.example.aichatbot.service.graph;

import lombok.Builder;
import org.bsc.langgraph4j.state.AgentState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
public class RagState extends AgentState {

    public RagState() {
        super(new HashMap<>());
    }

    public RagState(Map<String, Object> data) {
        super(data);
    }

    public String getQuery() {
        return (String) data().get("query");
    }

    @SuppressWarnings("unchecked")
    public List<String> getDocuments() {
        return (List<String>) data().get("documents");
    }

    public Boolean getIsRelevant() {
        return (Boolean) data().get("isRelevant");
    }

    public String getResponse() {
        return (String) data().get("response");
    }

    public String getConversationId() {
        return (String) data().get("conversationId");
    }

    public String getUserId() {
        return (String) data().get("userId");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getTokenUsage() {
        return (Map<String, Integer>) data().getOrDefault("tokenUsage", new HashMap<>());
    }

}
