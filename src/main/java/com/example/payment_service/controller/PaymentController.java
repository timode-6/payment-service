package com.example.payment_service.controller;

import com.example.payment_service.dto.responses.*;
import com.example.payment_service.dto.requests.*;
import com.example.payment_service.model.PaymentStatus;
import com.example.payment_service.security.UserPrincipal;
import com.example.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPaymentById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(paymentService.getPaymentById(id, principal));
    }

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getPayments(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) PaymentStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                paymentService.getPayments(userId, orderId, status, principal));
    }

    @GetMapping("/summary/me")
    public ResponseEntity<PaymentSummaryResponse> getSummaryForCurrentUser(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                paymentService.getSummaryForCurrentUser(from, to, principal));
    }

    @GetMapping("/summary/all")
    public ResponseEntity<PaymentSummaryResponse> getSummaryForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        return ResponseEntity.ok(paymentService.getSummaryForAllUsers(from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentDto> updatePayment(
            @PathVariable String id,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(paymentService.updatePayment(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}