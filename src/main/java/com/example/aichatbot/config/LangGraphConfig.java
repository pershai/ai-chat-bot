package com.example.aichatbot.config;

import com.example.aichatbot.service.graph.RagGraph;
import com.example.aichatbot.service.graph.RagState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangGraphConfig {

    @Bean
    public CompiledGraph<RagState> ragGraphRunner(RagGraph ragGraph) throws Exception {
        return ragGraph.buildGraph();
    }
}
