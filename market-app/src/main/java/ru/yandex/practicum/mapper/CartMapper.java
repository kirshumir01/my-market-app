package ru.yandex.practicum.mapper;

import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.item.ItemDto;

import java.util.List;

public class CartMapper {

    public static CartDto toCartDto(List<ItemDto> itemDtoList, long total) {
        return CartDto.builder()
                .items(itemDtoList)
                .total(total)
                .build();
    }
}
