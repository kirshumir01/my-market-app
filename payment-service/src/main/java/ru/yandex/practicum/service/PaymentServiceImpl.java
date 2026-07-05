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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final String currency;
    private final BigDecimal initialBalance;
    private final Map<Long, BigDecimal> balances = new ConcurrentHashMap<>();
    private final ReentrantLock paymentLock = new ReentrantLock();

    public PaymentServiceImpl(
            @Value("${payment.currency}") String currency,
            @Value("${payment.balance.initial}") BigDecimal initialBalance
    ) {
        this.currency = currency;
        this.initialBalance = initialBalance;
    }

    @Override
    public Mono<BalanceResponseDto> getBalance(long userId) {
        return Mono.fromSupplier(() -> new BalanceResponseDto(
                balances.getOrDefault(userId, initialBalance),
                currency
        ));
    }

    @Override
    public Mono<PaymentResponseDto> makePayment(PaymentRequestDto request) {
        return Mono.fromSupplier(() -> processPayment(request));
    }

    private PaymentResponseDto processPayment(PaymentRequestDto request) {
        paymentLock.lock();

        try {
            Long userId = request.getUserId();

            BigDecimal currentBalance = balances.getOrDefault(
                    userId,
                    initialBalance
            );

            if (currentBalance.compareTo(request.getAmount()) < 0) {
                return new PaymentResponseDto(
                        request.getOrderId(),
                        PaymentStatus.FAILED,
                        request.getAmount(),
                        currentBalance,
                        request.getCurrency(),
                        "Not enough money on balance",
                        Instant.now()
                );
            }

            BigDecimal remainingBalance = currentBalance.subtract(request.getAmount());

            balances.put(userId, remainingBalance);

            return new PaymentResponseDto(
                    request.getOrderId(),
                    PaymentStatus.PAID,
                    request.getAmount(),
                    remainingBalance,
                    request.getCurrency(),
                    "Payment completed successfully",
                    Instant.now()
            );
        } finally {
            paymentLock.unlock();
        }
    }
}