package com.example.payment_service.controller;

import com.example.payment_service.dto.requests.CreatePaymentRequest;
import com.example.payment_service.dto.requests.PaymentDto;
import com.example.payment_service.dto.requests.UpdatePaymentRequest;
import com.example.payment_service.dto.responses.PaymentSummaryResponse;
import com.example.payment_service.exception.DuplicatePaymentException;
import com.example.payment_service.exception.GlobalExceptionHandler;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.model.PaymentStatus;
import com.example.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
 
    @Mock
    private PaymentService paymentService;
 
    @InjectMocks
    private PaymentController controller;
 
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
 
    private static final String BASE_URL    = "/api/payments";
    private static final String USER_ID     = "user-1";
    private static final String ADMIN_ID    = "admin-1";
    private static final String ORDER_ID    = "order-1";
    private static final String PAYMENT_ID  = "pay-abc";

    private final Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
    private final Instant to = Instant.now();
 
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
 
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    private void asUser(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private void asAdmin(String adminId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }


    private PaymentDto sampleDto() {
        return PaymentDto.builder()
                .id(PAYMENT_ID)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .status(PaymentStatus.COMPLETED)
                .paymentAmount(9999L)
                .timestamp(Instant.now())
                .build();
    }

    private CreatePaymentRequest createRequest() {
        CreatePaymentRequest r = new CreatePaymentRequest();
        r.setOrderId(ORDER_ID);
        r.setUserId(USER_ID);
        r.setPaymentAmount(9999L);
        return r;
    }

    private PaymentSummaryResponse summaryResponse() {
        return PaymentSummaryResponse.builder()
                .totalAmount(19998L)
                .paymentCount(2L)
                .from(from).to(to)
                .userId(USER_ID)
                .build();
    }

    private UsernamePasswordAuthenticationToken mockUser(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

     private UsernamePasswordAuthenticationToken mockAdmin(String adminId) {
        return new UsernamePasswordAuthenticationToken(
                adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }


    @Test
    void userCreates_201() throws Exception {

        when(paymentService.createPayment(any(), any())).thenReturn(sampleDto());

        mockMvc.perform(post(BASE_URL)
                        .principal(mockUser(USER_ID)) 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(PAYMENT_ID));
    }

    @Test
        void adminCreates_201() throws Exception {
                
        when(paymentService.createPayment(any(), any())).thenReturn(sampleDto());

        mockMvc.perform(post(BASE_URL)
                        .principal(mockUser(USER_ID)) 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());
        }


   @Test
        void duplicateOrder_409() throws Exception {
                
        when(paymentService.createPayment(any(), any()))
                .thenThrow(new DuplicatePaymentException(ORDER_ID));

        mockMvc.perform(post(BASE_URL)
                        .principal(mockUser(USER_ID)) 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isConflict());
        }


    @Test
    void blankOrderId_400() throws Exception {
        asUser(USER_ID);
        CreatePaymentRequest bad = createRequest();
        bad.setOrderId("");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.orderId").exists());
    }

    @Test
    void negativeAmount_400() throws Exception {
        asUser(USER_ID);
        CreatePaymentRequest bad = createRequest();
        bad.setPaymentAmount(-1L);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.paymentAmount").exists());
    }

    @Test
    void nullAmount_400() throws Exception {
        asUser(USER_ID);
        CreatePaymentRequest bad = createRequest();
        bad.setPaymentAmount(null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidOrderIdPattern_400() throws Exception {
        asUser(USER_ID);
        CreatePaymentRequest bad = createRequest();
        bad.setOrderId("order id with spaces!");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.orderId").exists());
    }


        @Test
        void ownerGets_200() throws Exception {
                UsernamePasswordAuthenticationToken auth = mockUser(USER_ID); 
                when(paymentService.getPaymentById(eq(PAYMENT_ID), any())).thenReturn(sampleDto());

                mockMvc.perform(get(BASE_URL + "/{id}", PAYMENT_ID)
                                .principal(auth)) 
                        .andExpect(status().isOk());
        }

    @Test
    void adminGets_200() throws Exception {
        UsernamePasswordAuthenticationToken auth = mockAdmin(ADMIN_ID); 

        when(paymentService.getPaymentById(eq(PAYMENT_ID), any())).thenReturn(sampleDto());

        mockMvc.perform(get(BASE_URL + "/{id}", PAYMENT_ID)
                .principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void notFound_404() throws Exception {
        UsernamePasswordAuthenticationToken auth = mockUser(USER_ID); 

        when(paymentService.getPaymentById(eq("bad-id"), any()))
                .thenThrow(new PaymentNotFoundException("bad-id"));

        mockMvc.perform(get(BASE_URL + "/{id}", "bad-id").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("bad-id")));
    }


    @Test
    void byUserId_200() throws Exception {
        UsernamePasswordAuthenticationToken auth = mockUser(USER_ID); 

        when(paymentService.getPayments(eq(USER_ID), isNull(), isNull(), any()))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get(BASE_URL).param("userId", USER_ID).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));
    }

    @Test
    void byOrderId_200() throws Exception {
        UsernamePasswordAuthenticationToken auth = mockUser(USER_ID); 

        when(paymentService.getPayments(isNull(), eq(ORDER_ID), isNull(), any()))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get(BASE_URL).param("orderId", ORDER_ID).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void byStatus_200() throws Exception {
        UsernamePasswordAuthenticationToken auth = mockAdmin(ADMIN_ID); 

        when(paymentService.getPayments(isNull(), isNull(), eq(PaymentStatus.COMPLETED), any()))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get(BASE_URL).param("status", "COMPLETED").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void noFilter_400() throws Exception {
        when(paymentService.getPayments(isNull(), isNull(), isNull(), any()))
                .thenThrow(new IllegalArgumentException("Provide exactly one of: userId, orderId, status"));

        mockMvc.perform(get(BASE_URL).principal(mockUser(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Provide exactly one")));
    }

    @Test
    void multipleFilters_400() throws Exception {
        when(paymentService.getPayments(eq(USER_ID), eq(ORDER_ID), isNull(), any()))
                .thenThrow(new IllegalArgumentException("Only one filter parameter is allowed at a time"));

        mockMvc.perform(get(BASE_URL)
                        .param("userId", USER_ID)
                        .param("orderId", ORDER_ID)
                .principal(mockUser(USER_ID)))
                .andExpect(status().isBadRequest());
    }

    
    @Test
    void userSummary_200() throws Exception {
        when(paymentService.getSummaryForCurrentUser(any(), any(), any()))
                .thenReturn(summaryResponse());

        mockMvc.perform(get(BASE_URL + "/summary/me")
                        .param("from", from.toString())
                        .param("to",   to.toString())
                .principal(mockUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(19998L))
                .andExpect(jsonPath("$.paymentCount").value(2))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void adminSummary_200() throws Exception {
        when(paymentService.getSummaryForCurrentUser(any(), any(), any()))
                .thenReturn(summaryResponse());

        mockMvc.perform(get(BASE_URL + "/summary/me")
                        .param("from", from.toString())
                        .param("to",   to.toString())
                .principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isOk());
    }

    @Test
    void invalidRange_400() throws Exception {
        when(paymentService.getSummaryForCurrentUser(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("'from' must not be after 'to'"));

        mockMvc.perform(get(BASE_URL + "/summary/me")
                        .param("from", to.toString())
                        .param("to",   from.toString()).principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isBadRequest());
    }

    
    @Test
    void adminGlobalSummary_200() throws Exception {
        when(paymentService.getSummaryForAllUsers(any(), any()))
                .thenReturn(PaymentSummaryResponse.builder()
                        .totalAmount(9999L)
                        .paymentCount(100L)
                        .from(from).to(to)
                        .build());

        mockMvc.perform(get(BASE_URL + "/summary/all")
                        .param("from", Instant.now().minus(30, ChronoUnit.DAYS).toString())
                        .param("to",   Instant.now().toString()).principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(9999))
                .andExpect(jsonPath("$.paymentCount").value(100))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }


    @Test
    void adminUpdatesStatus_200() throws Exception {
        PaymentDto updated = sampleDto();
        updated.setStatus(PaymentStatus.FAILED);
        when(paymentService.updatePayment(eq(PAYMENT_ID), any(), any())).thenReturn(updated);

        UpdatePaymentRequest req = new UpdatePaymentRequest();
        req.setStatus(PaymentStatus.FAILED);

        mockMvc.perform(put(BASE_URL + "/{id}", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)).principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void adminUpdatesAmount_200() throws Exception {
        when(paymentService.updatePayment(eq(PAYMENT_ID), any(), any())).thenReturn(sampleDto());

        UpdatePaymentRequest req = new UpdatePaymentRequest();
        req.setPaymentAmount(14999L);

        mockMvc.perform(put(BASE_URL + "/{id}", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)).principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isOk());
    }

    @Test
    void paymentNotFound_404() throws Exception {
        when(paymentService.updatePayment(eq("bad-id"), any(), any()))
                .thenThrow(new PaymentNotFoundException("bad-id"));

        UpdatePaymentRequest req = new UpdatePaymentRequest();
        req.setStatus(PaymentStatus.FAILED);

        mockMvc.perform(put(BASE_URL + "/{id}", "bad-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)).principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void negativeAmountAdmin_400() throws Exception {
        asAdmin(ADMIN_ID);
        UpdatePaymentRequest bad = new UpdatePaymentRequest();
        bad.setPaymentAmount(-5L);

        mockMvc.perform(put(BASE_URL + "/{id}", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.paymentAmount").exists());
    }

        @Test
        void adminDeletes_204() throws Exception {
        when(paymentService.deletePayment(PAYMENT_ID)).thenReturn(null); 

        mockMvc.perform(delete(BASE_URL + "/{id}", PAYMENT_ID).principal(mockAdmin(ADMIN_ID)))
                .andExpect(status().isNoContent());

        verify(paymentService).deletePayment(PAYMENT_ID);
        }


    @Test
    void notFoundAdmin_404() throws Exception {
        asAdmin(ADMIN_ID);
        doThrow(new PaymentNotFoundException("bad-id"))
                .when(paymentService).deletePayment("bad-id");

        mockMvc.perform(delete(BASE_URL + "/{id}", "bad-id"))
                .andExpect(status().isNotFound());
    }
}