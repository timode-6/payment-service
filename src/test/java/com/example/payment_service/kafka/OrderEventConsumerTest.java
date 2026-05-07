package com.example.payment_service.kafka;

import com.example.payment_service.dto.requests.PaymentDto;
import com.example.payment_service.model.PaymentStatus;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.kafka.events.OrderCreatedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer")
class OrderEventConsumerTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private OrderEventConsumer consumer;

    private static final Long ORDER_ID = 99L;
    private static final Long USER_ID = 1L;

    private OrderCreatedEvent event;
    private PaymentDto paymentDto;

    @BeforeEach
    void setUp() {
        event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .userEmail("user@example.com")
                .totalAmount(14999L)
                .timestamp(Instant.now())
                .build();

        paymentDto = PaymentDto.builder()
                .id("pay-1")
                .orderId(ORDER_ID.toString())
                .userId(USER_ID.toString())
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(14999L)
                .timestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("valid ORDER_CREATED event → payment created, offset acknowledged")
    void validEvent_createsPaymentAndAcknowledges() {
        when(paymentService.createPaymentFromOrderEvent(event)).thenReturn(paymentDto);

        consumer.handleOrderCreated(event, "order.created", 0, 0L, ack);

        verify(paymentService).createPaymentFromOrderEvent(event);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("service returns FAILED payment → still acknowledges (status is business logic)")
    void failedPaymentStatus_stillAcknowledges() {
        PaymentDto failed = PaymentDto.builder()
                .id("pay-2").status(PaymentStatus.FAILED).build();
        when(paymentService.createPaymentFromOrderEvent(event)).thenReturn(failed);

        consumer.handleOrderCreated(event, "order.created", 1, 10L, ack);

        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("event delivered on any partition → processed correctly")
    void anyPartition_processedCorrectly() {
        when(paymentService.createPaymentFromOrderEvent(any())).thenReturn(paymentDto);

        assertThatNoException().isThrownBy(() -> consumer.handleOrderCreated(event, "order.created", 2, 999L, ack));

        verify(ack).acknowledge();
    }


    @Test
    @DisplayName("service throws RuntimeException → exception re-thrown, no ack (triggers retry)")
    void serviceThrows_noAck() {
        when(paymentService.createPaymentFromOrderEvent(event))
                .thenThrow(new RuntimeException("MongoDB unavailable"));

        assertThatThrownBy(() -> consumer.handleOrderCreated(event, "order.created", 0, 0L, ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MongoDB unavailable");

        verify(ack, never()).acknowledge();
    }

    @Test
    @DisplayName("duplicate event (idempotency handled in service) → service called, ack sent")
    void duplicateEvent_serviceHandlesIdempotency() {
        when(paymentService.createPaymentFromOrderEvent(event)).thenReturn(paymentDto);

        consumer.handleOrderCreated(event, "order.created", 0, 1L, ack);
        consumer.handleOrderCreated(event, "order.created", 0, 2L, ack);

        verify(paymentService, times(2)).createPaymentFromOrderEvent(event);
        verify(ack, times(2)).acknowledge();
    }

    @Test
    @DisplayName("IllegalArgumentException from service → re-thrown, no ack")
    void illegalArgument_noAck() {
        when(paymentService.createPaymentFromOrderEvent(event))
                .thenThrow(new IllegalArgumentException("Invalid orderId format"));

        assertThatThrownBy(() -> consumer.handleOrderCreated(event, "order.created", 0, 5L, ack))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    @DisplayName("event with minimal fields (no userEmail) → still processed")
    void minimalEvent_processed() {
        OrderCreatedEvent minimal = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .totalAmount(10L)
                .timestamp(Instant.now())
                .build();

        when(paymentService.createPaymentFromOrderEvent(minimal)).thenReturn(paymentDto);

        consumer.handleOrderCreated(minimal, "order.created", 0, 0L, ack);

        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("very large totalAmount → passes through without truncation")
    void largeAmount_processedCorrectly() {
        OrderCreatedEvent bigOrder = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .totalAmount(999999999L)
                .timestamp(Instant.now())
                .build();

        when(paymentService.createPaymentFromOrderEvent(bigOrder)).thenReturn(paymentDto);

        assertThatNoException().isThrownBy(() -> consumer.handleOrderCreated(bigOrder, "order.created", 0, 0L, ack));

        verify(ack).acknowledge();
    }
}