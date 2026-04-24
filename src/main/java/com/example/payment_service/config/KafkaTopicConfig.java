package com.example.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private String paymentCreatedTopic = "payment.created";

    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name(paymentCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}