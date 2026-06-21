package ru.yandex.practicum.items.mapper;

import ru.yandex.practicum.items.dto.ItemDto;
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
}
