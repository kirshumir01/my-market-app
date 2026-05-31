package ru.yandex.practicum.catalog.service;

import ru.yandex.practicum.items.dto.ItemsPageDto;
import ru.yandex.practicum.items.model.ItemSort;

public interface CatalogService {

    ItemsPageDto getItems(String search, ItemSort sort, int pageNumber, int pageSize);
}
