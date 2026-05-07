package com.example.payment_service.kafka;

import com.example.payment_service.kafka.events.CreatePaymentEvent;
import com.example.payment_service.model.PaymentStatus;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {
    private static final String TOPIC = "payment.created";
    @Mock
    private KafkaTemplate<String, CreatePaymentEvent> kafkaTemplate;
    private PaymentEventProducer producer;
    @BeforeEach
    void setUp() {
        producer = new PaymentEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "topic", TOPIC);
    }
    @Test
    void publishPaymentCreated_sendsToCorrectTopicAndKey() {
        CreatePaymentEvent event = sampleEvent();
        CompletableFuture<SendResult<String, CreatePaymentEvent>> future = successFuture();
        when(kafkaTemplate.send(anyString(), anyString(), any(CreatePaymentEvent.class)))
                .thenReturn(future);
        producer.publishPaymentCreated(event);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eq(event));
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("order-999");
    }
    @Test
    void publishPaymentCreated_doesNotThrow_onSuccessfulSend() {
        CreatePaymentEvent event = sampleEvent();
        CompletableFuture<SendResult<String, CreatePaymentEvent>> future = successFuture();
        when(kafkaTemplate.send(anyString(), anyString(), any(CreatePaymentEvent.class)))
                .thenReturn(future);
        assertThatNoException().isThrownBy(() -> producer.publishPaymentCreated(event));
    }
    @Test
    void publishPaymentCreated_doesNotThrow_whenFutureFails() {
        CreatePaymentEvent event = sampleEvent();
        CompletableFuture<SendResult<String, CreatePaymentEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(CreatePaymentEvent.class)))
                .thenReturn(failedFuture);
        assertThatNoException().isThrownBy(() -> producer.publishPaymentCreated(event));
    }
    @Test
    void publishPaymentCreated_invokesKafkaTemplateExactlyOnce() {
        CreatePaymentEvent event = sampleEvent();
        CompletableFuture<SendResult<String, CreatePaymentEvent>> future = successFuture();
        when(kafkaTemplate.send(anyString(), anyString(), any(CreatePaymentEvent.class)))
                .thenReturn(future);
        producer.publishPaymentCreated(event);
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(CreatePaymentEvent.class));
    }
    @Test
    void publishPaymentCreated_sendsTheExactEventObject() {
        CreatePaymentEvent event = sampleEvent();
        CompletableFuture<SendResult<String, CreatePaymentEvent>> future = successFuture();
        when(kafkaTemplate.send(anyString(), anyString(), any(CreatePaymentEvent.class)))
                .thenReturn(future);
        producer.publishPaymentCreated(event);
        ArgumentCaptor<CreatePaymentEvent> eventCaptor = ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isSameAs(event);
    }
    private CreatePaymentEvent sampleEvent() {
        CreatePaymentEvent event = mock(CreatePaymentEvent.class);
        when(event.getPaymentId()).thenReturn("pay-001");
        when(event.getOrderId()).thenReturn("order-999");
        when(event.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);
        return event;
    }
    private CompletableFuture<SendResult<String, CreatePaymentEvent>> successFuture() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 42L, 0, 0L, 0, 0);
        @SuppressWarnings("unchecked")
        SendResult<String, CreatePaymentEvent> result = mock(SendResult.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        return CompletableFuture.completedFuture(result);
    }
}
