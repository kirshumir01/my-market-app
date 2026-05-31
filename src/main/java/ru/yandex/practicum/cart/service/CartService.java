package ru.yandex.practicum.cart.service;

import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.model.CartAction;

public interface CartService {

    CartDto getCart();

    void changeItemsCount(long itemId, CartAction action);
}