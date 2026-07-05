package ru.yandex.practicum.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.order.OrderDto;

public interface OrderService {

    Flux<OrderDto> getOrders(String username);

    Mono<OrderDto> getOrder(String username, long orderId);

    Mono<Long> createOrderFromCart(String username);
}