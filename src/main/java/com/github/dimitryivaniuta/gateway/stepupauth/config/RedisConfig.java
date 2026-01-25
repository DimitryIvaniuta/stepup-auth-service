package com.github.dimitryivaniuta.gateway.stepupauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis configuration for OTP storage. */
@Configuration
public class RedisConfig {
    /** @return StringRedisTemplate */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
