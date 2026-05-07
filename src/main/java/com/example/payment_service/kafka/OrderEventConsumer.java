package com.example.payment_service.kafka;

import com.example.payment_service.service.PaymentService;
import com.example.payment_service.kafka.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${kafka.topics.order-created:order.created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received ORDER_CREATED event: eventId={} orderId={} userId={} amount={} [topic={} partition={} offset={}]",
                event.getEventId(), event.getOrderId(), event.getUserId(),
                event.getTotalAmount(), topic, partition, offset);

        try {
            paymentService.createPaymentFromOrderEvent(event);
            ack.acknowledge();
            log.debug("ORDER_CREATED event acknowledged: eventId={}", event.getEventId());
        } catch (Exception ex) {
            log.error("Failed to process ORDER_CREATED event eventId={} orderId={}: {}",
                    event.getEventId(), event.getOrderId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
