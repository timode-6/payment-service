package com.example.payment_service.kafka.events;

import com.example.payment_service.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentEvent {

    private String eventId;
    private String paymentId;
    private String orderId;
    private String userId;
    private PaymentStatus paymentStatus;
    private Long paymentAmount;
    private Instant timestamp;
}