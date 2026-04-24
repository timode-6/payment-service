package com.example.payment_service.dto.requests;

import com.example.payment_service.model.PaymentStatus;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdatePaymentRequest {

    private PaymentStatus status;

    @Min(value = 1, message = "Payment amount must be at least 1 cent")
    private Long paymentAmount;
}