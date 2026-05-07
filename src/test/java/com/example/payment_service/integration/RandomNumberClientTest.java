package com.example.payment_service.integration;

import com.example.payment_service.client.RandomNumberClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext 
class RandomNumberClientIntegrationTest extends AbstractIntegrationTest {

    static WireMockServer wireMock;

    @DynamicPropertySource
    static void wireRandomServiceUrl(DynamicPropertyRegistry registry) {
        if (wireMock == null) {
            wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            wireMock.start();
        }
        
        registry.add("random-number-service.url", 
            () -> "http://localhost:" + wireMock.port());
    }


    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Autowired
    RandomNumberClient randomNumberClient;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    @DisplayName("even number response → isEven() returns true")
    void evenResponse_returnsTrue() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"number": 42, "isEven": true}
                                """)));

        assertThat(randomNumberClient.isEven()).isTrue();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/random")));
    }

    @Test
    @DisplayName("odd number response → isEven() returns false")
    void oddResponse_returnsFalse() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"number": 7, "isEven": false}
                                """)));

        assertThat(randomNumberClient.isEven()).isFalse();
    }

    @Test
    @DisplayName("HTTP 500 from random-service → fallback returns false")
    void serverError_fallsBackToFalse() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse().withStatus(500)));

        assertThat(randomNumberClient.isEven()).isFalse();
    }

    @Test
    @DisplayName("HTTP 503 from random-service → fallback returns false")
    void serviceUnavailable_fallsBackToFalse() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse().withStatus(503)));

        assertThat(randomNumberClient.isEven()).isFalse();
    }

    @Test
    @DisplayName("empty body from random-service → fallback returns false")
    void emptyBody_fallsBackToFalse() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")));

        assertThat(randomNumberClient.isEven()).isFalse();
    }

    @Test
    @DisplayName("connection refused / fixed delay → fallback returns false")
    void connectionRefused_fallsBackToFalse() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse().withFault(
                        com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        assertThat(randomNumberClient.isEven()).isFalse();
    }

    @Test
    @DisplayName("client calls exactly /api/random — not the root or any other path")
    void callsCorrectPath() {
        wireMock.stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            {"number": 2, "isEven": true}
                        """)));

        randomNumberClient.isEven();

        wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/api/random")));
        wireMock.verify(exactly(0), getRequestedFor(urlMatching("(?!/api/random).*")));
    }
}