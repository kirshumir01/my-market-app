package ru.yandex.practicum.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.payment.BalanceResponseDto;
import ru.yandex.practicum.dto.payment.PaymentRequestDto;
import ru.yandex.practicum.dto.payment.PaymentResponseDto;

@Service
@RequiredArgsConstructor
public class PaymentClient {

    private final WebClient paymentServiceWebClient;

    public Mono<BalanceResponseDto> getBalance(Long userId) {
        return paymentServiceWebClient.get()
                .uri("/api/v1/balance/{userId}", userId)
                .retrieve()
                .bodyToMono(BalanceResponseDto.class);
    }

    public Mono<PaymentResponseDto> makePayment(PaymentRequestDto request) {
        return paymentServiceWebClient.post()
                .uri("/api/v1/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponseDto.class);
    }
}