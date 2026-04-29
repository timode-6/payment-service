package com.example.payment_service.controller;

import com.example.payment_service.dto.requests.CreatePaymentRequest;
import com.example.payment_service.dto.requests.PaymentDto;
import com.example.payment_service.dto.responses.PaymentSummaryResponse;
import com.example.payment_service.dto.requests.UpdatePaymentRequest;
import com.example.payment_service.model.PaymentStatus;
import com.example.payment_service.security.UserPrincipal;
import com.example.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<PaymentDto> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request, principal(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<PaymentDto> getPaymentById(
            @PathVariable String id,
            Authentication authentication) {

        return ResponseEntity.ok(paymentService.getPaymentById(id, principal(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<PaymentDto>> getPayments(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) PaymentStatus status,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getPayments(userId, orderId, status, principal(authentication)));
    }

    @GetMapping("/summary/me")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<PaymentSummaryResponse> getSummaryForCurrentUser(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getSummaryForCurrentUser(from, to, principal(authentication)));
    }

    @GetMapping("/summary/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentSummaryResponse> getSummaryForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        return ResponseEntity.ok(paymentService.getSummaryForAllUsers(from, to));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDto> updatePayment(
            @PathVariable String id,
            @Valid @RequestBody UpdatePaymentRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.updatePayment(id, request, principal(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    private UserPrincipal principal(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        return UserPrincipal.of(userId, role);
    }
}