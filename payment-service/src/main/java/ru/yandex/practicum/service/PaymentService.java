package ru.yandex.practicum.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.BalanceResponseDto;
import ru.yandex.practicum.dto.PaymentRequestDto;
import ru.yandex.practicum.dto.PaymentResponseDto;

public interface PaymentService {

    Mono<BalanceResponseDto> getBalance(long userId);

    Mono<PaymentResponseDto> makePayment(PaymentRequestDto request);
}