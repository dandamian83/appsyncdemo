package com.example.appsyncdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class Config {

    @Bean
    public WebClient.RequestBodySpec getWebClient( @Value("${AWS_APP_SYNC_URL}") String appSyncUrl, @Value("${AWS_APP_SYNC_API_KEY}") String apiKey) {
        return WebClient
                .builder()
                .baseUrl(appSyncUrl)
                .defaultHeader("x-api-key", apiKey)
                .build()
                .method(HttpMethod.POST)
                .uri("");
    }
}
