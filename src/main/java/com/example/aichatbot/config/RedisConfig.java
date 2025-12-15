package com.example.aichatbot.config;

import com.example.aichatbot.model.IngestionJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, IngestionJob> jobRedisTemplate(RedisConnectionFactory connectionFactory,
            org.springframework.core.env.Environment env,
            ObjectMapper objectMapper) {
        // TODO remove this later
//        String host = env.getProperty("spring.data.redis.host");
//        String port = env.getProperty("spring.data.redis.port");
//        System.out.println(">>> CONNECTING TO REDIS AT: " + host + ":" + port);

        RedisTemplate<String, IngestionJob> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<IngestionJob> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                IngestionJob.class);

        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
