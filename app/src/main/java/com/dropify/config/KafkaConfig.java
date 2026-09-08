package com.dropify.config;

import com.dropify.event.KafkaTopic;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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

    @Bean
    public NewTopic paymentCompletedDlqTopic() {
        return TopicBuilder.name(KafkaTopic.PAYMENT_COMPLETED_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedDlqTopic() {
        return TopicBuilder.name(KafkaTopic.PAYMENT_FAILED_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCancelledDlqTopic() {
        return TopicBuilder.name(KafkaTopic.PAYMENT_CANCELLED_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledDlqTopic() {
        return TopicBuilder.name(KafkaTopic.ORDER_CANCELLED_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", 0));

        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
