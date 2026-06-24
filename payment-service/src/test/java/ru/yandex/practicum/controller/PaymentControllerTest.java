package ru.yandex.practicum.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
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

@WebFluxTest(controllers = PaymentController.class)
@Import(ErrorHandler.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getBalanceReturnsPayloadFromService() {
        when(paymentService.getBalance())
                .thenReturn(Mono.just(new BalanceResponseDto(
                        BigDecimal.valueOf(10000),
                        "RUB"
                )));

        webTestClient.get()
                .uri("/api/v1/balance")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.balance").isEqualTo(10000)
                .jsonPath("$.currency").isEqualTo("RUB");

        verify(paymentService).getBalance();
        verifyNoMoreInteractions(paymentService);
    }

    @Test
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
        assertThat(actualRequest.getAmount()).isEqualByComparingTo("2499.90");
        assertThat(actualRequest.getCurrency()).isEqualTo("RUB");

        verifyNoMoreInteractions(paymentService);
    }

    @Test
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
    void postPaymentWhenAmountIsZeroReturnsBadRequest() {
        String request = """
                {
                  "orderId": 1001,
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
                .jsonPath("$.error").isEqualTo("Validation failed")
                .jsonPath("$.description").exists();

        verifyNoInteractions(paymentService);
    }

    @Test
    void postPaymentWhenCurrencyIsBlankReturnsBadRequest() {
        String request = """
                {
                  "orderId": 1001,
                  "amount": 2499.90,
                  "currency": ""
                }
                """;

        webTestClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Validation failed")
                .jsonPath("$.description").exists();

        verifyNoInteractions(paymentService);
    }

    @Test
    void postPaymentWhenBodyIsMalformedReturnsBadRequest() {
        String request = """
                {
                  "orderId": 1001,
                  "amount":
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

        verifyNoInteractions(paymentService);
    }
}
