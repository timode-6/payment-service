package com.example.payment_service.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CreatePaymentRequest {

    @NotBlank(message = "Order ID is required")
    @Size(min = 1, max = 64, message = "Order ID must be between 1 and 64 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Order ID must contain only alphanumeric characters, hyphens or underscores"
    )
    private String orderId;

    @NotBlank(message = "User ID is required")
    @Size(min = 1, max = 64, message = "User ID must be between 1 and 64 characters")
    private String userId;

    @NotNull(message = "Payment amount is required")
    @Min(value = 1, message = "Payment amount must be at least 1 cent")
    private Long paymentAmount;
}