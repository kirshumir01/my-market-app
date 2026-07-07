package ru.yandex.practicum.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentClientOauth2Test {

    private MockWebServer mockWebServer;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .filter((request, next) -> next.exchange(
                        ClientRequest
                                .from(request)
                                .headers(headers -> headers.setBearerAuth("test-access-token"))
                                .build()
                ))
                .build();

        paymentClient = new PaymentClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("getBalance(userId) -> sends request with Bearer token")
    void getBalance_shouldSendBearerToken() throws InterruptedException {
        Long userId = 1L;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "balance": 10000.00,
                          "currency": "RUB"
                        }
                        """));

        StepVerifier.create(paymentClient.getBalance(userId))
                .assertNext(response -> {
                    assertThat(response.getBalance()).isEqualByComparingTo("10000.00");
                    assertThat(response.getCurrency()).isEqualTo("RUB");
                })
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/v1/balance/1");
        assertThat(request.getHeader("Authorization"))
                .isEqualTo("Bearer test-access-token");
    }

    @Test
    @DisplayName("getBalance(userId) -> returns Unauthorized error when payment service responds 401")
    void getBalance_whenTokenExpiredOrInvalid_shouldReturnUnauthorizedError() {
        Long userId = 1L;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "error": "invalid_token",
                          "error_description": "Access token expired"
                        }
                        """));

        StepVerifier.create(paymentClient.getBalance(userId))
                .expectErrorSatisfies(error -> {
                    assertThat(error)
                            .isInstanceOf(WebClientResponseException.Unauthorized.class);

                    WebClientResponseException exception =
                            (WebClientResponseException) error;

                    assertThat(exception.getStatusCode().value())
                            .isEqualTo(401);
                })
                .verify();
    }
}