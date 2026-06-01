package ru.yandex.practicum.items.mapper;

import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.dto.ItemShortDto;
import ru.yandex.practicum.items.model.Item;

public class ItemMapper {

    public static ItemDto toItemDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .imgPath(item.getImgPath())
                .price(item.getPrice())
                .count(0)
                .build();
    }

    public static ItemDto toItemEmptyDto() {
        return ItemDto.builder()
                .id(-1L)
                .title("")
                .description("")
                .imgPath("")
                .price(0L)
                .count(0)
                .build();
    }

    public static ItemDto itemDto(CartItem cartItem) {
        return ItemDto.builder()
                .id(cartItem.getItem().getId())
                .title(cartItem.getItem().getTitle())
                .description(cartItem.getItem().getDescription())
                .imgPath(cartItem.getItem().getImgPath())
                .price(cartItem.getItem().getPrice())
                .count(cartItem.getCount())
                .build();
    }

    public static ItemShortDto toItemShortDto(Item item) {
        return ItemShortDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .price(0L)
                .count(0)
                .build();
    }
}
