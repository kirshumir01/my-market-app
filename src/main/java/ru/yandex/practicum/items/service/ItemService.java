package ru.yandex.practicum.items.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.items.dto.ItemDto;

public interface ItemService {

    Mono<ItemDto> getItem(long itemId);
}
