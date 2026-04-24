package com.example.payment_service.exception;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String orderId) {
        super("A payment for order '" + orderId + "' already exists");
    }
}