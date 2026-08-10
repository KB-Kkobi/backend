package org.kkobi.external.kis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KisApiConfig {

    @Value("${kis.api.base-url}")
    private String baseUrl;

    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    @Value("${kis.ws.base-url}")
    private String wsBaseUrl;

    @Bean(name = "kisRestTemplate")
    public RestTemplate kisRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }

    @Bean
    public KisApiProperties kisApiProperties() {
        return new KisApiProperties(baseUrl, appKey, appSecret, wsBaseUrl);
    }

    public record KisApiProperties(
            String baseUrl,
            String appKey,
            String appSecret,
            String wsBaseUrl
    ) {}
}
