package com.dropify.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "toss")
public class TossPaymentProperties {
    private String secretKey;
    private String baseUrl;
    private String webhookSecret;
}
