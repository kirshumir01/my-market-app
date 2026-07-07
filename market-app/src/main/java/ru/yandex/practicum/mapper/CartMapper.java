package ru.yandex.practicum.mapper;

import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.cart.CartViewDto;
import ru.yandex.practicum.dto.item.ItemDto;

import java.math.BigDecimal;
import java.util.List;

public class CartMapper {

    public static CartDto toCartDto(List<ItemDto> itemDtoList, long total) {
        return CartDto.builder()
                .items(itemDtoList)
                .total(total)
                .build();
    }

    public static CartViewDto toCartViewDto(
            CartDto cart,
            BigDecimal balance,
            String currency,
            boolean paymentError,
            boolean paymentServiceError
    ) {
        boolean canOrder = !cart.getItems().isEmpty()
                && balance != null
                && balance.compareTo(BigDecimal.valueOf(cart.getTotal())) >= 0;

        return new CartViewDto(
                cart.getItems(),
                cart.getTotal(),
                balance,
                currency,
                canOrder,
                paymentError,
                paymentServiceError
        );
    }
}
