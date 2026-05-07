package com.example.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
 
    @Value("${kafka.topics.payment-created:payment.created}")
    private String paymentCreatedTopic;
 
    @Value("${kafka.topics.order-created:order.created}")
    private String orderCreatedTopic;
 
    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name(paymentCreatedTopic)
                .partitions(3).replicas(1).build();
    }
 
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreatedTopic)
                .partitions(3).replicas(1).build();
    }
}
 