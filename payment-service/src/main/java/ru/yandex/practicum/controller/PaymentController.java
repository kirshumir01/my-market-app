package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.BalanceResponseDto;
import ru.yandex.practicum.dto.PaymentRequestDto;
import ru.yandex.practicum.dto.PaymentResponseDto;
import ru.yandex.practicum.service.PaymentService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/balance")
    public Mono<BalanceResponseDto> getBalance() {
        return paymentService.getBalance();
    }

    @PostMapping("/payments")
    public Mono<ResponseEntity<PaymentResponseDto>> makePayment(
            @Valid @RequestBody PaymentRequestDto request
    ) {
        return paymentService.makePayment(request).map(ResponseEntity::ok);
    }
}