package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.dto.PaymentRequestDto;
import ru.yandex.practicum.dto.PaymentStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PaymentServiceImplTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                "RUB",
                BigDecimal.valueOf(10000)
        );
    }

    @Test
    @DisplayName("getBalance() -> returns current balance")
    void getBalanceReturnsCurrentBalance() {
        StepVerifier.create(paymentService.getBalance())
                .expectNextMatches(response ->
                        response.getBalance().compareTo(BigDecimal.valueOf(10000)) == 0
                                && response.getCurrency().equals("RUB"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> returns PAID and updates balance when money is enough")
    void makePaymentWhenEnoughMoneyReturnsPaidAndChangesBalance() {
        PaymentRequestDto request = new PaymentRequestDto(
                1L,
                BigDecimal.valueOf(2500),
                "RUB"
        );

        StepVerifier.create(paymentService.makePayment(request))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
                    assertThat(response.getRemainingBalance())
                            .isEqualByComparingTo("7500");
                    assertThat(response.getMessage())
                            .isEqualTo("Payment completed successfully");
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .assertNext(balance ->
                        assertThat(balance.getBalance())
                                .isEqualByComparingTo("7500"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> returns FAILED and keeps balance when money is not enough")
    void makePaymentWhenNotEnoughMoneyReturnsFailedAndDoesNotChangeBalance() {
        PaymentRequestDto request = new PaymentRequestDto(
                1L,
                BigDecimal.valueOf(15000),
                "RUB"
        );

        StepVerifier.create(paymentService.makePayment(request))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(response.getRemainingBalance())
                            .isEqualByComparingTo("10000");
                    assertThat(response.getMessage())
                            .isEqualTo("Not enough money on balance");
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .assertNext(balance ->
                        assertThat(balance.getBalance())
                                .isEqualByComparingTo("10000"))
                .verifyComplete();
    }
}