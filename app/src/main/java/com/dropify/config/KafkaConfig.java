package com.dropify.config;

import com.dropify.event.KafkaTopic;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(KafkaTopic.PAYMENT_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(KafkaTopic.PAYMENT_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCancelledTopic() {
        return TopicBuilder.name(KafkaTopic.PAYMENT_CANCELLED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(KafkaTopic.ORDER_CANCELLED).partitions(3).replicas(1).build();
    }

}
