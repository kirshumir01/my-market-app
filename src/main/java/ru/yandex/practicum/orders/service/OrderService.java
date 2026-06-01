package ru.yandex.practicum.orders.service;

import ru.yandex.practicum.orders.dto.OrderDto;

import java.util.List;

public interface OrderService {
    List<OrderDto> getOrders();

    OrderDto getOrder(long orderId);

    long createOrderFromCart();
}
