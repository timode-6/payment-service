package com.example.payment_service.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private Instant timestamp;
    private Map<String, String> fieldErrors; 
}