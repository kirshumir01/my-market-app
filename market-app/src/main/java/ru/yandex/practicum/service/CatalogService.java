package ru.yandex.practicum.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.item.ItemsPageDto;
import ru.yandex.practicum.model.ItemSort;

public interface CatalogService {

    Mono<ItemsPageDto> getItems(
            String username,
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize
    );
}
