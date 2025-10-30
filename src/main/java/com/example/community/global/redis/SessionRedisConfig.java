package com.example.community.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class SessionRedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private Integer port;

    // Redis 연결 팩토리 설정
    @Bean(name = "sessionRedisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis 설정 - host와 port가 필요함
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);

        // Lettuce vs Jedis ⇒ Lettuce 선택, Lettuce 라이브러리를 사용해서 Redis에 연결
        // Lettuce는 Jedis보다 성능이 좋고 비동기 처리가 가능함
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    /**
     * RedisTemplate 설정 - key: String - value: JSON 직렬화 (세션 데이터나 객체 저장용)
     */
    @Bean(name = "sessionRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        // Key 직렬화
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Value 직렬화 (JSON)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
