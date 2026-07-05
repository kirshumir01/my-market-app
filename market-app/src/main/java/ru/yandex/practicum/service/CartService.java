package ru.yandex.practicum.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.cart.CartViewDto;
import ru.yandex.practicum.model.CartAction;

public interface CartService {

    Mono<CartDto> getCart(String username);

    Mono<Void> changeItemsCount(String username, long itemId, CartAction action);

    Mono<CartViewDto> getCartView(String username, boolean paymentError);
}