package com.example.payment_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.payment_service.dto.responses.RandomNumberResponse;

@Slf4j
@Component
public class RandomNumberClient {

    private static final String RANDOM_PATH = "/api/random";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RandomNumberClient(
            RestTemplate restTemplate,
            @Value("${random-number-service.url}") String baseUrl) {

        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
    }

    public boolean isEven() {
        String url = baseUrl + RANDOM_PATH;
        try {
            RandomNumberResponse response = restTemplate.getForObject(url, RandomNumberResponse.class);
            if (response == null) {
                log.warn("random-number-service returned null body; defaulting to FAILED");
                return false;
            }

            log.debug("random-number-service returned number={} isEven={}",
                    response.getNumber(), response.isEven());
            return response.isEven();

        } catch (RestClientException e) {
            log.error("Failed to reach random-number-service at {}: {}; defaulting to FAILED",
                    url, e.getMessage());
            return false;
        }
    }
}