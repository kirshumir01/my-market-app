package ru.yandex.practicum.catalog.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.items.dto.ItemsPageDto;
import ru.yandex.practicum.items.model.ItemSort;

public interface CatalogService {

    Mono<ItemsPageDto> getItems(String search, ItemSort sort, int pageNumber, int pageSize);
}
