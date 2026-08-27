package com.dropify.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
@EnableConfigurationProperties(TossPaymentProperties.class)
public class TossPaymentConfig {

    @Bean
    public WebClient tossWebClient(TossPaymentProperties properties) {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes());
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
