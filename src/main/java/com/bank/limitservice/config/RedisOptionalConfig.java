package com.bank.limitservice.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * When Redis is disabled, exclude Redis auto-configuration so the app starts without a Redis server.
 */
@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
public class RedisOptionalConfig {
}
