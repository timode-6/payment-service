package com.example.payment_service.kafka;

import com.example.payment_service.kafka.events.CreatePaymentEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, CreatePaymentEvent> kafkaTemplate;

    private String topic = "payment.created";

    public void publishPaymentCreated(CreatePaymentEvent event) {
        log.info("Publishing CREATE_PAYMENT event: paymentId={} orderId={} status={}",
                event.getPaymentId(), event.getOrderId(), event.getPaymentStatus());

        CompletableFuture<SendResult<String, CreatePaymentEvent>> future = kafkaTemplate.send(topic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish CREATE_PAYMENT event for orderId={}: {}", event.getOrderId(), ex.getMessage(), ex);
            } else {
                log.debug("CREATE_PAYMENT event delivered → topic={} partition={} offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}