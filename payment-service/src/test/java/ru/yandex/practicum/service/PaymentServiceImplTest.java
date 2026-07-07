package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.dto.PaymentRequestDto;
import ru.yandex.practicum.dto.PaymentStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PaymentServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                "RUB",
                BigDecimal.valueOf(10000)
        );
    }

    @Test
    @DisplayName("getBalance(userId) -> returns current user balance")
    void getBalanceReturnsCurrentBalance() {
        StepVerifier.create(paymentService.getBalance(USER_ID))
                .expectNextMatches(response ->
                        response.getBalance().compareTo(BigDecimal.valueOf(10000)) == 0
                                && response.getCurrency().equals("RUB"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> returns PAID and updates user balance when money is enough")
    void makePaymentWhenEnoughMoneyReturnsPaidAndChangesBalance() {
        PaymentRequestDto request = new PaymentRequestDto(
                1L,
                USER_ID,
                BigDecimal.valueOf(2500),
                "RUB"
        );

        StepVerifier.create(paymentService.makePayment(request))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
                    assertThat(response.getRemainingBalance()).isEqualByComparingTo("7500");
                    assertThat(response.getMessage()).isEqualTo("Payment completed successfully");
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(balance ->
                        assertThat(balance.getBalance()).isEqualByComparingTo("7500"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> returns FAILED and keeps user balance when money is not enough")
    void makePaymentWhenNotEnoughMoneyReturnsFailedAndDoesNotChangeBalance() {
        PaymentRequestDto request = new PaymentRequestDto(
                1L,
                USER_ID,
                BigDecimal.valueOf(15000),
                "RUB"
        );

        StepVerifier.create(paymentService.makePayment(request))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(response.getRemainingBalance()).isEqualByComparingTo("10000");
                    assertThat(response.getMessage()).isEqualTo("Not enough money on balance");
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(balance ->
                        assertThat(balance.getBalance()).isEqualByComparingTo("10000"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> keeps balances independent for different users")
    void makePayment_shouldKeepUserBalancesIndependent() {
        PaymentRequestDto firstUserRequest = new PaymentRequestDto(
                1L,
                USER_ID,
                BigDecimal.valueOf(2500),
                "RUB"
        );

        StepVerifier.create(paymentService.makePayment(firstUserRequest))
                .expectNextMatches(response -> response.getStatus() == PaymentStatus.PAID)
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(balance ->
                        assertThat(balance.getBalance()).isEqualByComparingTo("7500"))
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(OTHER_USER_ID))
                .assertNext(balance ->
                        assertThat(balance.getBalance()).isEqualByComparingTo("10000"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> processes concurrent payments sequentially for one user")
    void makePayment_shouldProcessConcurrentRequestsSafely() {
        PaymentRequestDto request = new PaymentRequestDto(
                1L,
                USER_ID,
                BigDecimal.valueOf(3000),
                "RUB"
        );

        Flux.merge(
                        paymentService.makePayment(request),
                        paymentService.makePayment(request),
                        paymentService.makePayment(request)
                )
                .collectList()
                .as(StepVerifier::create)
                .assertNext(responses -> {
                    long paid = responses.stream()
                            .filter(r -> r.getStatus() == PaymentStatus.PAID)
                            .count();

                    assertThat(paid).isEqualTo(3);
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(balance ->
                        assertThat(balance.getBalance()).isEqualByComparingTo("1000"))
                .verifyComplete();
    }

    @Test
    @DisplayName("makePayment() -> prevents race condition during concurrent payments for one user")
    void makePayment_shouldBeThreadSafe() {
        PaymentRequestDto request = new PaymentRequestDto(
                1L,
                USER_ID,
                BigDecimal.valueOf(3000),
                "RUB"
        );

        StepVerifier.create(
                        Flux.merge(
                                paymentService.makePayment(request),
                                paymentService.makePayment(request),
                                paymentService.makePayment(request),
                                paymentService.makePayment(request)
                        ).collectList()
                )
                .assertNext(results -> {
                    long paid = results.stream()
                            .filter(r -> r.getStatus() == PaymentStatus.PAID)
                            .count();

                    long failed = results.stream()
                            .filter(r -> r.getStatus() == PaymentStatus.FAILED)
                            .count();

                    assertThat(paid).isEqualTo(3);
                    assertThat(failed).isEqualTo(1);
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(balance ->
                        assertThat(balance.getBalance()).isEqualByComparingTo("1000"))
                .verifyComplete();
    }
}