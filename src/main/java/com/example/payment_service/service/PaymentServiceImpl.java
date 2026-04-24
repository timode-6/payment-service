package com.example.payment_service.service;

import com.example.payment_service.client.RandomNumberClient;
import com.example.payment_service.dto.requests.CreatePaymentRequest;
import com.example.payment_service.dto.requests.PaymentDto;
import com.example.payment_service.dto.responses.PaymentSummaryResponse;
import com.example.payment_service.dto.requests.UpdatePaymentRequest;
import com.example.payment_service.exception.DuplicatePaymentException;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.kafka.events.CreatePaymentEvent;
import com.example.payment_service.kafka.PaymentEventProducer;
import com.example.payment_service.mapper.PaymentMapper;
import com.example.payment_service.model.Payment;
import com.example.payment_service.model.PaymentStatus;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer eventProducer;

    private static final String TOTAL_AMOUNT = "totalAmount";
    private static final String PAYMENT_COUNT = "paymentCount";

    @Override
    public PaymentDto createPayment(CreatePaymentRequest request, UserPrincipal principal) {
        if (!principal.isAdmin() && !principal.getUserId().equals(request.getUserId())) {
            throw new AccessDeniedException("You can only create payments for your own account");
        }
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new DuplicatePaymentException(request.getOrderId());
        }

        Payment payment = paymentMapper.fromCreateRequest(request);
        payment.setStatus(randomNumberClient.isEven() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);

        Payment saved = paymentRepository.save(payment);
        log.info("Created payment {} (status={}) for order {} by user {}",
                saved.getId(), saved.getStatus(), saved.getOrderId(), saved.getUserId());

        publishCreatePaymentEvent(saved);
        return paymentMapper.toDto(saved);
    }

    @Override
    public PaymentDto getPaymentById(String id, UserPrincipal principal) {
        Payment payment = findOrThrow(id);
        assertOwnerOrAdmin(payment.getUserId(), principal);
        return paymentMapper.toDto(payment);
    }

    @Override
    public List<PaymentDto> getPayments(
            String userId, String orderId, PaymentStatus status, UserPrincipal principal) {

        long filled = countNonNull(userId, orderId, status);
        if (filled == 0) throw new IllegalArgumentException("Provide exactly one of: userId, orderId, status");
        if (filled > 1)  throw new IllegalArgumentException("Only one filter parameter is allowed at a time");

        if (userId != null) {
            if (!principal.isAdmin() && !principal.getUserId().equals(userId)) {
                throw new AccessDeniedException("You can only view your own payments");
            }
            return paymentMapper.toDtoList(paymentRepository.findByUserId(userId));
        }

        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId)
                    .filter(p -> principal.isAdmin() || principal.getUserId().equals(p.getUserId()))
                    .map(p -> List.of(paymentMapper.toDto(p)))
                    .orElseGet(List::of);
        }

        if (!principal.isAdmin()) {
            return paymentMapper.toDtoList(
                    paymentRepository.findByUserIdAndStatus(principal.getUserId(), status));
        }
        return paymentMapper.toDtoList(paymentRepository.findByStatus(status));
    }

    @Override
    public PaymentSummaryResponse getSummaryForCurrentUser(Instant from, Instant to, UserPrincipal principal) {
        validateDateRange(from, to);
        return buildSummary(principal.getUserId(), from, to);
    }

    @Override
    public PaymentSummaryResponse getSummaryForAllUsers(Instant from, Instant to) {
        validateDateRange(from, to);
        return buildSummary(null, from, to);
    }

    @Override
    public PaymentDto updatePayment(String id, UpdatePaymentRequest request, UserPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new AccessDeniedException("Only admins can update payments");
        }
        if (request.getStatus() == null && request.getPaymentAmount() == null) {
            throw new IllegalArgumentException(
                    "At least one of 'status' or 'paymentAmount' must be provided");
        }

        Payment payment = findOrThrow(id);
        PaymentDto patch = PaymentDto.builder()
                .status(request.getStatus())
                .paymentAmount(request.getPaymentAmount())
                .build();
        paymentMapper.applyUpdate(patch, payment);

        Payment updated = paymentRepository.save(payment);
        log.info("Admin {} updated payment {}", principal.getUserId(), id);
        return paymentMapper.toDto(updated);
    }

    @Override
    public PaymentDto deletePayment(String id) {
        Payment payment = findOrThrow(id);
        paymentMapper.applySoftDelete(payment);
        Payment saved = paymentRepository.save(payment);
        log.info("Soft-deleted payment {}", id);
        return paymentMapper.toDto(saved);
    }


    private void publishCreatePaymentEvent(Payment payment) {
        CreatePaymentEvent event = CreatePaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paymentStatus(payment.getStatus())
                .paymentAmount(payment.getPaymentAmount())
                .timestamp(payment.getTimestamp() != null ? payment.getTimestamp() : Instant.now())
                .build();
        eventProducer.publishPaymentCreated(event);
    }

    private Payment findOrThrow(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void assertOwnerOrAdmin(String ownerId, UserPrincipal principal) {
        if (!principal.isAdmin() && !principal.getUserId().equals(ownerId)) {
            throw new AccessDeniedException("Access denied to this payment");
        }
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both 'from' and 'to' dates are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }

    private PaymentSummaryResponse buildSummary(String userId, Instant from, Instant to) {
        Criteria criteria = Criteria.where("deleted").ne(true)
                .and("status").is(PaymentStatus.COMPLETED)
                .and("timestamp").gte(from).lte(to);
        if (userId != null) {
            criteria = criteria.and("user_id").is(userId);
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group()
                        .sum("payment_amount").as(TOTAL_AMOUNT)
                        .count().as(PAYMENT_COUNT)
        );

        Map<?, ?> result = mongoTemplate
                .aggregate(aggregation, "payments", Map.class)
                .getUniqueMappedResult();

        Long total = result != null && result.get(TOTAL_AMOUNT) != null
                ? Long.parseLong(result.get(TOTAL_AMOUNT).toString()) : 0L;
        Long count = result != null && result.get(PAYMENT_COUNT) != null
                ? Long.parseLong(result.get(PAYMENT_COUNT).toString()) : 0L;

        return PaymentSummaryResponse.builder()
                .totalAmount(total)
                .paymentCount(count)
                .from(from)
                .to(to)
                .userId(userId)
                .build();
    }

    private long countNonNull(Object... values) {
        long count = 0;
        for (Object v : values) { if (v != null) count++; }
        return count;
    }
}