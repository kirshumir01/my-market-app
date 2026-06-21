package ru.yandex.practicum.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.items.model.ItemSort;

@Getter
@Setter
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