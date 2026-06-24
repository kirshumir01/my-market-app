package ru.yandex.practicum.mapper;

import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.model.Item;

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

    public static ItemCardCacheDto toItemCardCacheDto(Item item) {
        return ItemCardCacheDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .imgPath(item.getImgPath())
                .build();
    }

    public static ItemDto toItemDto(ItemCardCacheDto item) {
        return ItemDto.builder()
                .id(item.getId())
                .imgPath(item.getImgPath())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .build();
    }
}
