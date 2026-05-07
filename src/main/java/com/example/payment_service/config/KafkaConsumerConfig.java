package com.example.payment_service.config;

import com.example.payment_service.kafka.events.OrderCreatedEvent;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

        @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
        private String bootstrapServers;

        @Value("${kafka.consumer.group-id:payment-service-group}")
        private String groupId;

        @Bean
        public ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory(KafkaProperties properties) {
                JacksonJsonDeserializer<OrderCreatedEvent> jsonDeserializer = new JacksonJsonDeserializer<>(OrderCreatedEvent.class);
                jsonDeserializer.addTrustedPackages("*");
                jsonDeserializer.setUseTypeHeaders(false);

                return new DefaultKafkaConsumerFactory<>(
                                properties.buildConsumerProperties(),
                                new StringDeserializer(),
                                new ErrorHandlingDeserializer<>(jsonDeserializer));
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedListenerContainerFactory(
                        ConsumerFactory<String, OrderCreatedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                factory.setRecordMessageConverter(new StringJacksonJsonMessageConverter());

                factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
                factory.setConcurrency(3);
                factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2_000L, 3L)));

                return factory;
        }

}
