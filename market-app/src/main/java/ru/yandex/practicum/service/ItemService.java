package ru.yandex.practicum.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.item.ItemRequest;

public interface ItemService {

    Mono<ItemDto> getItem(long itemId);

    Mono<ItemDto> createItem(ItemRequest request);
}