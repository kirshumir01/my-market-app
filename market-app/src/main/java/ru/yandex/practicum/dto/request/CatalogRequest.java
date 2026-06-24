package ru.yandex.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.ItemSort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogRequest {
    private Long itemId;
    private CartAction action;
    private String search;
    private ItemSort sort;
    private int pageNumber;
    private int pageSize;
}