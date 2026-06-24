package ru.yandex.practicum.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import ru.yandex.practicum.dto.payment.PaymentRequestDto;
import ru.yandex.practicum.dto.payment.PaymentStatus;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentClientTest {

    private MockWebServer mockWebServer;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        paymentClient = new PaymentClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getBalance_shouldReturnBalance() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "balance": 10000.00,
                          "currency": "RUB"
                        }
                        """));

        StepVerifier.create(paymentClient.getBalance())
                .assertNext(response -> {
                    assertThat(response.getBalance())
                            .isEqualByComparingTo("10000.00");
                    assertThat(response.getCurrency()).isEqualTo("RUB");
                })
                .verifyComplete();
    }

    @Test
    void makePayment_shouldReturnPaymentResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "orderId": null,
                          "status": "PAID",
                          "amount": 2499.90,
                          "remainingBalance": 7500.10,
                          "currency": "RUB",
                          "message": "Payment completed successfully",
                          "paymentTime": "2026-06-23T12:30:00Z"
                        }
                        """));

        PaymentRequestDto request = new PaymentRequestDto(
                null,
                BigDecimal.valueOf(2499.90),
                "RUB"
        );

        StepVerifier.create(paymentClient.makePayment(request))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
                    assertThat(response.getAmount()).isEqualByComparingTo("2499.90");
                    assertThat(response.getRemainingBalance()).isEqualByComparingTo("7500.10");
                    assertThat(response.getCurrency()).isEqualTo("RUB");
                })
                .verifyComplete();
    }
}