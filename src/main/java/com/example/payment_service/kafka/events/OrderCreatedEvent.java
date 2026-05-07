package com.example.payment_service.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor 
public class OrderCreatedEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("orderId")
    private Long orderId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("totalAmount")
    private Long totalAmount;

   @JsonProperty("timestamp")
    private Instant timestamp; 

}