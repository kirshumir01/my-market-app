package ru.yandex.practicum.orders.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.orders.dto.OrderDto;

public interface OrderService {

    Flux<OrderDto> getOrders();

    Mono<OrderDto> getOrder(long orderId);

    Mono<Long> createOrderFromCart();
}
