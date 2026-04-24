package com.example.payment_service.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PaymentSummaryResponse {

    private Long      totalAmount;   
    private Long      paymentCount; 
    private Instant   from;
    private Instant   to;
    private String    userId;
}