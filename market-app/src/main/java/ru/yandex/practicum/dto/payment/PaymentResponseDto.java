package ru.yandex.practicum.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private String currency;
    private String message;
    private Instant paymentTime;
}