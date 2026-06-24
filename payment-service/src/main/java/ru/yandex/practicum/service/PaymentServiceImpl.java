package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.BalanceResponseDto;
import ru.yandex.practicum.dto.PaymentRequestDto;
import ru.yandex.practicum.dto.PaymentResponseDto;
import ru.yandex.practicum.dto.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final String currency;
    private final AtomicReference<BigDecimal> balance;

    public PaymentServiceImpl(
            @Value("${payment.currency}") String currency,
            @Value("${payment.balance.initial}") BigDecimal initialBalance
    ) {
        this.currency = currency;
        this.balance = new AtomicReference<>(initialBalance);
    }

    @Override
    public Mono<BalanceResponseDto> getBalance() {
        return Mono.just(
                new BalanceResponseDto(
                        balance.get(),
                        currency)
        );
    }

    @Override
    public Mono<PaymentResponseDto> makePayment(PaymentRequestDto request) {

        Instant paymentTime = Instant.now();

        while (true) {
            BigDecimal currentBalance = balance.get();

            if (currentBalance.compareTo(request.getAmount()) < 0) {
                return Mono.just(
                        new PaymentResponseDto(
                                request.getOrderId(),
                                PaymentStatus.FAILED,
                                request.getAmount(),
                                currentBalance,
                                request.getCurrency(),
                                "Not enough money on balance",
                                paymentTime
                        )
                );
            }

            BigDecimal remainingBalance = currentBalance.subtract(request.getAmount());

            if (balance.compareAndSet(currentBalance, remainingBalance)) {
                return Mono.just(
                        new PaymentResponseDto(
                                request.getOrderId(),
                                PaymentStatus.PAID,
                                request.getAmount(),
                                remainingBalance,
                                request.getCurrency(),
                                "Payment completed successfully",
                                paymentTime
                        )
                );
            }
        }
    }
}
