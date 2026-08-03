package org.kkobi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    private final String redisHost;
    private final int redisPort;

    public RedisConfig(
            @Value("${redis.host}") String redisHost,
            @Value("${redis.port}") int redisPort
    ) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration redisConfiguration =
                new RedisStandaloneConfiguration();

        redisConfiguration.setHostName(redisHost);
        redisConfiguration.setPort(redisPort);

        return new LettuceConnectionFactory(redisConfiguration);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ){
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
