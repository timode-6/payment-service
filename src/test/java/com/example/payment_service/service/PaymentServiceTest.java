package com.example.payment_service.service;

import com.example.payment_service.client.RandomNumberClient;
import com.example.payment_service.dto.requests.CreatePaymentRequest;
import com.example.payment_service.dto.requests.PaymentDto;
import com.example.payment_service.dto.requests.UpdatePaymentRequest;
import com.example.payment_service.dto.responses.PaymentSummaryResponse;
import com.example.payment_service.exception.DuplicatePaymentException;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.kafka.events.CreatePaymentEvent;
import com.example.payment_service.kafka.PaymentEventProducer;
import com.example.payment_service.mapper.PaymentMapper;
import com.example.payment_service.model.Payment;
import com.example.payment_service.model.PaymentStatus;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.security.UserPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository    paymentRepository;
    @Mock private MongoTemplate        mongoTemplate;
    @Mock private PaymentMapper        paymentMapper;
    @Mock private RandomNumberClient   randomNumberClient;
    @Mock private PaymentEventProducer eventProducer;

    @InjectMocks private PaymentServiceImpl service;


    private static final String ADMIN_ID = "admin-1";
    private static final String USER_ID  = "user-1";
    private static final String ORDER_ID = "order-1";
    private static final String PAY_ID   = "pay-1";

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private Payment       entity;
    private PaymentDto    dto;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(ADMIN_ID, "ADMIN");
        userPrincipal  = new UserPrincipal(USER_ID,  "USER");

        entity = Payment.builder()
                .id(PAY_ID)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .status(PaymentStatus.COMPLETED)
                .paymentAmount(9999L)
                .timestamp(Instant.now())
                .build();

        dto = PaymentDto.builder()
                .id(PAY_ID)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .status(PaymentStatus.COMPLETED)
                .paymentAmount(9999L)
                .timestamp(entity.getTimestamp())
                .build();
    }


    @Test
    void evenNumber_completedStatus_eventPublished() {
        CreatePaymentRequest req = buildCreateRequest(USER_ID, 9999L);
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentMapper.fromCreateRequest(req)).thenReturn(entity);
        when(randomNumberClient.isEven()).thenReturn(true);
        when(paymentRepository.save(entity)).thenReturn(entity);
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        PaymentDto result = service.createPayment(req, userPrincipal);

        assertThat(result.getId()).isEqualTo(PAY_ID);
        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        ArgumentCaptor<CreatePaymentEvent> eventCaptor =
                ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(eventProducer).publishPaymentCreated(eventCaptor.capture());

        CreatePaymentEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getEventId()).isNotBlank();
        assertThat(publishedEvent.getPaymentId()).isEqualTo(PAY_ID);
        assertThat(publishedEvent.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(publishedEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(publishedEvent.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(publishedEvent.getPaymentAmount()).isEqualByComparingTo(9999L);
    }

    @Test
    void oddNumber_failedStatus_eventPublished() {
        Payment freshEntity = Payment.builder()
                .id(PAY_ID).orderId(ORDER_ID).userId(USER_ID)
                .paymentAmount(5000L).timestamp(Instant.now()).build();

        CreatePaymentRequest req = buildCreateRequest(USER_ID, 5000L);
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentMapper.fromCreateRequest(req)).thenReturn(freshEntity);
        when(randomNumberClient.isEven()).thenReturn(false);
        when(paymentRepository.save(freshEntity)).thenReturn(freshEntity);
        when(paymentMapper.toDto(freshEntity)).thenReturn(
                PaymentDto.builder().status(PaymentStatus.FAILED).build());

        service.createPayment(req, userPrincipal);

        assertThat(freshEntity.getStatus()).isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<CreatePaymentEvent> captor =
                ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(eventProducer).publishPaymentCreated(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void userCreatesOwnPayment() {
        CreatePaymentRequest req = buildCreateRequest(USER_ID, 5000L);
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentMapper.fromCreateRequest(req)).thenReturn(entity);
        when(randomNumberClient.isEven()).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(entity);
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        assertThatNoException().isThrownBy(() -> service.createPayment(req, userPrincipal));
        verify(eventProducer).publishPaymentCreated(any());
    }

    @Test
    void userCreatesForOtherUser() {
        CreatePaymentRequest req = buildCreateRequest("other-user", 5000L);

        assertThatThrownBy(() -> service.createPayment(req, userPrincipal))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(paymentRepository, randomNumberClient, paymentMapper, eventProducer);
    }

    @Test
    void adminCreatesForAnyUser() {
        CreatePaymentRequest req = buildCreateRequest("other-user", 5000L);
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentMapper.fromCreateRequest(req)).thenReturn(entity);
        when(randomNumberClient.isEven()).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(entity);
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        assertThatNoException().isThrownBy(() -> service.createPayment(req, adminPrincipal));
        verify(eventProducer).publishPaymentCreated(any());
    }

    @Test
    void duplicateOrder_noEvent() {
        CreatePaymentRequest req = buildCreateRequest(USER_ID, 5000L);
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createPayment(req, userPrincipal))
                .isInstanceOf(DuplicatePaymentException.class);

        verifyNoInteractions(randomNumberClient, eventProducer);
    }

    @Test
    @DisplayName("Go service unavailable (fallback false) → FAILED status, event still published")
    void goServiceFallback_failedEvent() {
        Payment newEntity = Payment.builder()
                .id(PAY_ID).orderId(ORDER_ID).userId(USER_ID)
                .paymentAmount(1000L).timestamp(Instant.now()).build();

        CreatePaymentRequest req = buildCreateRequest(USER_ID, 1000L);
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentMapper.fromCreateRequest(req)).thenReturn(newEntity);
        when(randomNumberClient.isEven()).thenReturn(false);
        when(paymentRepository.save(newEntity)).thenReturn(newEntity);
        when(paymentMapper.toDto(newEntity)).thenReturn(
                PaymentDto.builder().status(PaymentStatus.FAILED).build());

        service.createPayment(req, userPrincipal);

        assertThat(newEntity.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventProducer).publishPaymentCreated(any());
    }

    @Test
    void ownerGetsOwnPayment() {
        when(paymentRepository.findById(PAY_ID)).thenReturn(Optional.of(entity));
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        assertThat(service.getPaymentById(PAY_ID, userPrincipal).getUserId())
                .isEqualTo(USER_ID);
    }

@Test
void nonOwnerBlocked() {
    when(paymentRepository.findById(PAY_ID)).thenReturn(Optional.of(entity));
    
    UserPrincipal stranger = new UserPrincipal("stranger", "USER");

    assertThatThrownBy(() -> service.getPaymentById(PAY_ID, stranger))
            .isInstanceOf(AccessDeniedException.class);
}


    @Test
    void adminGetsAnyPayment() {
        when(paymentRepository.findById(PAY_ID)).thenReturn(Optional.of(entity));
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        assertThatNoException()
                .isThrownBy(() -> service.getPaymentById(PAY_ID, adminPrincipal));
    }

    @Test
    void notFound() {
        when(paymentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentById("bad-id", adminPrincipal))
                .isInstanceOf(PaymentNotFoundException.class);
    }


    @Test
    void byOwnUserId() {
        when(paymentRepository.findByUserId(USER_ID)).thenReturn(List.of(entity));
        when(paymentMapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(service.getPayments(USER_ID, null, null, userPrincipal)).hasSize(1);
    }

    @Test
    void byOtherUserId() {
        assertThatThrownBy(() ->
            service.getPayments("other-user", null, null, userPrincipal))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void byOwnedOrderId() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(entity));
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        assertThat(service.getPayments(null, ORDER_ID, null, userPrincipal)).hasSize(1);
    }

    @Test
    void byForeignOrderId() {
        Payment foreign = Payment.builder().id("x").orderId(ORDER_ID)
                .userId("someone-else").status(PaymentStatus.COMPLETED)
                .paymentAmount(1000L).timestamp(Instant.now()).build();
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(foreign));

        assertThat(service.getPayments(null, ORDER_ID, null, userPrincipal)).isEmpty();
    }

    @Test
    void byStatusScopedUser() {
        when(paymentRepository.findByUserIdAndStatus(USER_ID, PaymentStatus.COMPLETED))
                .thenReturn(List.of(entity));
        when(paymentMapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(service.getPayments(null, null, PaymentStatus.COMPLETED, userPrincipal))
                .hasSize(1);
        verify(paymentRepository).findByUserIdAndStatus(USER_ID, PaymentStatus.COMPLETED);
    }

    @Test
    void byStatusAdmin() {
        when(paymentRepository.findByStatus(PaymentStatus.FAILED)).thenReturn(List.of(entity));
        when(paymentMapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(service.getPayments(null, null, PaymentStatus.FAILED, adminPrincipal))
                .hasSize(1);
        verify(paymentRepository).findByStatus(PaymentStatus.FAILED);
    }

    @Test
    void noFilter() {
        assertThatThrownBy(() -> service.getPayments(null, null, null, userPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provide exactly one");
    }

    @Test
    void multipleFilters() {
        assertThatThrownBy(() -> service.getPayments(USER_ID, ORDER_ID, null, userPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only one filter");
    }


    private final Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
    private final Instant to   = Instant.now();

    @SuppressWarnings("unchecked")
    private void mockAggregation(Long total, long count) {
        var mockResults = mock(AggregationResults.class);
        when(mockResults.getUniqueMappedResult())
                .thenReturn(Map.of("totalAmount", total, "paymentCount", count));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Map.class)))
                .thenReturn(mockResults);
    }

    @Test
    void currentUserSummary() {
        mockAggregation(25000L, 3L);

        PaymentSummaryResponse resp =
                service.getSummaryForCurrentUser(from, to, userPrincipal);

        assertThat(resp.getTotalAmount()).isEqualByComparingTo(25000L);
        assertThat(resp.getPaymentCount()).isEqualTo(3L);
        assertThat(resp.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("all-users summary (admin) → userId is null")
    void allUsersSummary() {
        mockAggregation(500000L, 42L);

        PaymentSummaryResponse resp = service.getSummaryForAllUsers(from, to);

        assertThat(resp.getTotalAmount()).isEqualByComparingTo(500000L);
        assertThat(resp.getUserId()).isNull();
    }

    @Test
    @DisplayName("from after to → IllegalArgumentException")
    void invalidDateRange() {
        assertThatThrownBy(() ->
            service.getSummaryForCurrentUser(to, from, userPrincipal))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'from' must not be after 'to'");
    }

    @Test
    @DisplayName("no matching payments → totalAmount=0, count=0")
    void noMatches() {
        var empty = mock(AggregationResults.class);
        when(empty.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Map.class)))
                .thenReturn(empty);

        PaymentSummaryResponse resp =
                service.getSummaryForCurrentUser(from, to, userPrincipal);

        assertThat(resp.getTotalAmount()).isEqualByComparingTo(0L);
        assertThat(resp.getPaymentCount()).isZero();
    }


    @Test
    void adminUpdatesStatus() {
        UpdatePaymentRequest req = new UpdatePaymentRequest();
        req.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findById(PAY_ID)).thenReturn(Optional.of(entity));
        when(paymentRepository.save(entity)).thenReturn(entity);
        when(paymentMapper.toDto(entity)).thenReturn(dto);

        service.updatePayment(PAY_ID, req, adminPrincipal);

        verify(paymentMapper).applyUpdate(any(PaymentDto.class), eq(entity));
        verify(paymentRepository).save(entity);
    }

    @Test
    void emptyRequest() {
        UpdatePaymentRequest emptyRequest = new UpdatePaymentRequest();

        assertThatThrownBy(() -> service.updatePayment(PAY_ID, emptyRequest, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one");
    }


    @Test
    void userUpdateBlocked() {
        UpdatePaymentRequest request = new UpdatePaymentRequest();

        assertThatThrownBy(() -> service.updatePayment(PAY_ID, request, userPrincipal))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(paymentRepository, paymentMapper);
    }


    @Test
    void adminDeletes() {
        when(paymentRepository.findById(PAY_ID)).thenReturn(Optional.of(entity));

        assertThatNoException().isThrownBy(() -> service.deletePayment(PAY_ID));
         verify(paymentRepository).save(entity);
    }

    @Test
    void paymentDoesNotExist() {
        when(paymentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePayment("bad-id"))
                .isInstanceOf(PaymentNotFoundException.class);
        verify(paymentRepository, never()).delete(any());
    }


    private CreatePaymentRequest buildCreateRequest(String userId, Long amount) {
        CreatePaymentRequest r = new CreatePaymentRequest();
        r.setUserId(userId);
        r.setOrderId(ORDER_ID);
        r.setPaymentAmount(amount);
        return r;
    }
}