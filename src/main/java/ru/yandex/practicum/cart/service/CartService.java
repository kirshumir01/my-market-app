package ru.yandex.practicum.cart.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.model.CartAction;

public interface CartService {

    Mono<CartDto> getCart();

    Mono<Void> changeItemsCount(long itemId, CartAction action);
}