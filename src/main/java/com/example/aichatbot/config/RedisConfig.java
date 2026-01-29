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
        @SuppressWarnings("deprecation")
        public RedisTemplate<String, IngestionJob> jobRedisTemplate(RedisConnectionFactory connectionFactory,
                        ObjectMapper objectMapper) {
                RedisTemplate<String, IngestionJob> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                template.setKeySerializer(new StringRedisSerializer());

                Jackson2JsonRedisSerializer<IngestionJob> serializer = new Jackson2JsonRedisSerializer<>(
                                objectMapper, IngestionJob.class);

                template.setValueSerializer(serializer);
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setHashValueSerializer(serializer);

                template.afterPropertiesSet();
                return template;
        }
}
