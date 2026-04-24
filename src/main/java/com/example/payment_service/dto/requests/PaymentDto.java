package com.example.payment_service.dto.requests;

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
public class PaymentDto {

    private String id;
    private String orderId;
    private String userId;
    private PaymentStatus status;
    private Long paymentAmount;
    private Instant timestamp;
    private Instant updatedAt;
}