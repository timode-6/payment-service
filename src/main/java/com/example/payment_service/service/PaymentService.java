package com.example.payment_service.service;

import com.example.payment_service.dto.requests.*;
import com.example.payment_service.dto.responses.*;
import com.example.payment_service.kafka.events.OrderCreatedEvent;
import com.example.payment_service.model.*;
import com.example.payment_service.security.*;

import java.util.List;
import java.time.Instant;  

public interface PaymentService {

    PaymentDto createPaymentFromOrderEvent(OrderCreatedEvent event);

    PaymentDto createPayment(CreatePaymentRequest request, UserPrincipal principal);

    PaymentDto getPaymentById(String id, UserPrincipal principal);

    List<PaymentDto> getPayments(
            String userId,
            String orderId,
            PaymentStatus status,
            UserPrincipal principal);

    PaymentSummaryResponse getSummaryForCurrentUser(Instant from, Instant to, UserPrincipal principal);

    PaymentSummaryResponse getSummaryForAllUsers(Instant from, Instant to);

    PaymentDto updatePayment(String id, UpdatePaymentRequest request, UserPrincipal principal);

    void deletePayment(String id);
}