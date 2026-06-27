package ru.yandex.practicum.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.dto.PaymentStatus;
import ru.yandex.practicum.exception.ErrorHandler;

@Import(ErrorHandler.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /api/v1/balance -> 200 OK with current balance")
    void getBalance_shouldReturnCurrentBalance() {
        webTestClient.get()
                .uri("/api/v1/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(10000)
                .jsonPath("$.currency").isEqualTo("RUB");
    }

    @Test
    @DisplayName("POST /api/v1/payments -> 200 OK with PAID payment status")
    void makePayment_whenEnoughMoney_shouldReturnPaidStatus() {
        String request = """
                {
                  "orderId": null,
                  "amount": 2499.90,
                  "currency": "RUB"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").doesNotExist()
                .jsonPath("$.status").isEqualTo(PaymentStatus.PAID.name())
                .jsonPath("$.amount").isEqualTo(2499.90)
                .jsonPath("$.remainingBalance").isEqualTo(7500.10)
                .jsonPath("$.currency").isEqualTo("RUB")
                .jsonPath("$.message").isEqualTo("Payment completed successfully")
                .jsonPath("$.paymentTime").exists();
    }

    @Test
    @DisplayName("POST /api/v1/payments -> 200 OK with FAILED payment status")
    void makePayment_whenNotEnoughMoney_shouldReturnFailedStatus() {
        String request = """
                {
                  "orderId": null,
                  "amount": 15000.00,
                  "currency": "RUB"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").doesNotExist()
                .jsonPath("$.status").isEqualTo(PaymentStatus.FAILED.name())
                .jsonPath("$.amount").isEqualTo(15000.00)
                .jsonPath("$.remainingBalance").isEqualTo(10000)
                .jsonPath("$.currency").isEqualTo("RUB")
                .jsonPath("$.message").isEqualTo("Not enough money on balance")
                .jsonPath("$.paymentTime").exists();
    }

    @Test
    @DisplayName("POST /api/v1/payments with invalid amount -> 400 BAD REQUEST")
    void makePayment_whenAmountIsInvalid_shouldReturnBadRequest() {
        String request = """
                {
                  "orderId": null,
                  "amount": 0,
                  "currency": "RUB"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").exists()
                .jsonPath("$.description").exists();
    }
}