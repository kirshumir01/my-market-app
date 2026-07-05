package ru.yandex.practicum.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.config.PaymentSecurityConfig;
import ru.yandex.practicum.dto.BalanceResponseDto;
import ru.yandex.practicum.dto.PaymentRequestDto;
import ru.yandex.practicum.dto.PaymentResponseDto;
import ru.yandex.practicum.dto.PaymentStatus;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = PaymentController.class)
@Import({
        PaymentSecurityConfig.class,
        ErrorHandler.class
})
@ActiveProfiles("test")
class PaymentControllerMockTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    @DisplayName("GET /api/v1/balance/{userId} -> 200 OK with current balance")
    void getBalanceReturnsPayloadFromService() {
        when(paymentService.getBalance(USER_ID))
                .thenReturn(Mono.just(new BalanceResponseDto(
                        BigDecimal.valueOf(10000),
                        "RUB"
                )));

        webTestClient
                .mutateWith(mockJwt())
                .get()
                .uri("/api/v1/balance/{userId}", USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.balance").isEqualTo(10000)
                .jsonPath("$.currency").isEqualTo("RUB");

        verify(paymentService).getBalance(USER_ID);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /api/v1/payments -> 200 OK with successful payment response")
    void postPaymentDelegatesToServiceAndReturnsJson() {
        Instant paymentTime = Instant.parse("2026-06-23T12:30:00Z");

        when(paymentService.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        1001L,
                        PaymentStatus.PAID,
                        BigDecimal.valueOf(2499.90),
                        BigDecimal.valueOf(7500.10),
                        "RUB",
                        "Payment completed successfully",
                        paymentTime
                )));

        String request = """
                {
                  "orderId": 1001,
                  "userId": 1,
                  "amount": 2499.90,
                  "currency": "RUB"
                }
                """;

        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.orderId").isEqualTo(1001)
                .jsonPath("$.status").isEqualTo("PAID")
                .jsonPath("$.amount").isEqualTo(2499.9)
                .jsonPath("$.remainingBalance").isEqualTo(7500.1)
                .jsonPath("$.currency").isEqualTo("RUB")
                .jsonPath("$.message").isEqualTo("Payment completed successfully")
                .jsonPath("$.paymentTime").isEqualTo("2026-06-23T12:30:00Z");

        ArgumentCaptor<PaymentRequestDto> captor =
                ArgumentCaptor.forClass(PaymentRequestDto.class);

        verify(paymentService).makePayment(captor.capture());

        PaymentRequestDto actualRequest = captor.getValue();

        assertThat(actualRequest.getOrderId()).isEqualTo(1001L);
        assertThat(actualRequest.getUserId()).isEqualTo(USER_ID);
        assertThat(actualRequest.getAmount()).isEqualByComparingTo("2499.90");
        assertThat(actualRequest.getCurrency()).isEqualTo("RUB");

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /api/v1/payments -> 200 OK with failed payment response")
    void postPaymentWhenPaymentFailedReturnsFailedStatus() {
        Instant paymentTime = Instant.parse("2026-06-23T12:31:00Z");

        when(paymentService.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        1001L,
                        PaymentStatus.FAILED,
                        BigDecimal.valueOf(2499.90),
                        BigDecimal.valueOf(1000.00),
                        "RUB",
                        "Not enough money on balance",
                        paymentTime
                )));

        String request = """
                {
                  "orderId": 1001,
                  "userId": 1,
                  "amount": 2499.90,
                  "currency": "RUB"
                }
                """;

        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo(1001)
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.amount").isEqualTo(2499.9)
                .jsonPath("$.remainingBalance").isEqualTo(1000.0)
                .jsonPath("$.currency").isEqualTo("RUB")
                .jsonPath("$.message").isEqualTo("Not enough money on balance")
                .jsonPath("$.paymentTime").isEqualTo("2026-06-23T12:31:00Z");

        verify(paymentService).makePayment(any(PaymentRequestDto.class));
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /api/v1/payments with zero amount -> 400 BAD REQUEST")
    void postPaymentWhenAmountIsZeroReturnsBadRequest() {
        String request = """
                {
                  "orderId": 1001,
                  "userId": 1,
                  "amount": 0,
                  "currency": "RUB"
                }
                """;

        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /api/v1/payments without userId -> 400 BAD REQUEST")
    void postPaymentWhenUserIdIsMissingReturnsBadRequest() {
        String request = """
                {
                  "orderId": 1001,
                  "amount": 2499.90,
                  "currency": "RUB"
                }
                """;

        webTestClient
                .mutateWith(mockJwt())
                .post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("GET /api/v1/balance/{userId} anonymous -> 401 UNAUTHORIZED")
    void getBalanceWithoutToken_shouldReturnUnauthorized() {
        webTestClient
                .get()
                .uri("/api/v1/balance/{userId}", USER_ID)
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /api/v1/payments anonymous -> 401 UNAUTHORIZED")
    void postPaymentWithoutToken_shouldReturnUnauthorized() {
        String request = """
                {
                  "orderId": 1001,
                  "userId": 1,
                  "amount": 2499.90,
                  "currency": "RUB"
                }
                """;

        webTestClient
                .post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(paymentService);
    }
}