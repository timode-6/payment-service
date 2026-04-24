package com.example.payment_service.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RandomNumberResponse {

    @JsonProperty("number")
    private int number;

    @JsonProperty("isEven")
    private boolean even;
}