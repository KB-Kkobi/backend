package org.kkobi.securities.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class QuoteConfig {

    // 다건 시세 조회 시 KIS API 병렬 호출용 전용 스레드풀
    @Bean(name = "kisQuoteExecutor", destroyMethod = "shutdown")
    public ExecutorService kisQuoteExecutor() {
        return Executors.newFixedThreadPool(8);
    }

    // Redis 시세 캐시 (Instant 포함) 직렬화용 ObjectMapper
    @Bean(name = "quoteObjectMapper")
    public ObjectMapper quoteObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
