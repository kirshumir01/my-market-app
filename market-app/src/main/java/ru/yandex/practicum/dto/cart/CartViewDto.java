package ru.yandex.practicum.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.item.ItemDto;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartViewDto {
    private List<ItemDto> items;
    private long total;
    private BigDecimal balance;
    private String currency;
    private boolean canOrder;
    private boolean paymentError;
    private boolean paymentServiceError;
}