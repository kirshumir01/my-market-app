package ru.yandex.practicum.cart.mapper;

import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.items.dto.ItemDto;

import java.util.List;

public class CartMapper {

    public static CartDto toCartDto(List<ItemDto> itemDtoList, long total) {
        return CartDto.builder()
                .items(itemDtoList)
                .total(total)
                .build();
    }
}
