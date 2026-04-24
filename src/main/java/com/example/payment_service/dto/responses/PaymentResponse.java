package com.example.payment_service.dto.responses;

import com.example.payment_service.model.Payment;
import com.example.payment_service.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PaymentResponse {

    private String id;
    private String orderId;
    private String userId;
    private PaymentStatus status;
    private Instant timestamp;
    private Instant updatedAt;
    private Long paymentAmount;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .status(payment.getStatus())
                .timestamp(payment.getTimestamp())
                .updatedAt(payment.getUpdatedAt())
                .paymentAmount(payment.getPaymentAmount())
                .build();
    }
}