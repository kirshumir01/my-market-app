package ru.yandex.practicum.items.service;

import ru.yandex.practicum.items.dto.ItemDto;

public interface ItemService {

    ItemDto getItem(long itemId);
}
