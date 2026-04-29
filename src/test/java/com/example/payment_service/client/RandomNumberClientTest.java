package com.example.payment_service.client;

import com.example.payment_service.dto.responses.RandomNumberResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomNumberClientTest {

    private static final String BASE_URL = "http://random-service";
    private static final String EXPECTED_URL = BASE_URL + "/api/random";

    @Mock
    private RestTemplate restTemplate;

    private RandomNumberClient client;

    @BeforeEach
    void setUp() {
        client = new RandomNumberClient(restTemplate, BASE_URL);
    }

    @Test
    void isEven_returnsTrue_whenServiceRespondsWithEvenNumber() {
        RandomNumberResponse response = mock(RandomNumberResponse.class);
        when(response.isEven()).thenReturn(true);
        when(restTemplate.getForObject(EXPECTED_URL, RandomNumberResponse.class)).thenReturn(response);

        assertThat(client.isEven()).isTrue();
    }

    @Test
    void isEven_returnsFalse_whenServiceRespondsWithOddNumber() {
        RandomNumberResponse response = mock(RandomNumberResponse.class);
        when(response.isEven()).thenReturn(false);
        when(restTemplate.getForObject(EXPECTED_URL, RandomNumberResponse.class)).thenReturn(response);

        assertThat(client.isEven()).isFalse();
    }

    @Test
    void isEven_returnsFalse_whenServiceReturnsNullBody() {
        when(restTemplate.getForObject(EXPECTED_URL, RandomNumberResponse.class)).thenReturn(null);

        assertThat(client.isEven()).isFalse();
    }

    @Test
    void isEven_returnsFalse_whenRestClientExceptionIsThrown() {
        when(restTemplate.getForObject(EXPECTED_URL, RandomNumberResponse.class))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThat(client.isEven()).isFalse();
    }

    @Test
    void isEven_returnsFalse_whenGenericRestClientExceptionIsThrown() {
        when(restTemplate.getForObject(EXPECTED_URL, RandomNumberResponse.class))
                .thenThrow(new RestClientException("Unexpected error"));

        assertThat(client.isEven()).isFalse();
    }

    @Test
    void isEven_callsCorrectUrl() {
        when(restTemplate.getForObject(EXPECTED_URL, RandomNumberResponse.class)).thenReturn(null);

        client.isEven();

        verify(restTemplate, times(1)).getForObject(EXPECTED_URL, RandomNumberResponse.class);
    }
}