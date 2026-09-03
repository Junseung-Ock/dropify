package com.dropify.concurrency;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "com.dropify.common",
                "com.dropify.product",
                "com.dropify.order",
                "com.dropify.payment",
                "com.dropify.web.usecase",
                "com.dropify.web.service"
        },
        exclude = {KafkaAutoConfiguration.class}
)
@EnableJpaRepositories(basePackages = {
        "com.dropify.common",
        "com.dropify.product",
        "com.dropify.order",
        "com.dropify.payment"
})
@EntityScan(basePackages = {
        "com.dropify.common",
        "com.dropify.product",
        "com.dropify.order",
        "com.dropify.payment"
})
public class AppIntegrationTestApplication {
}
