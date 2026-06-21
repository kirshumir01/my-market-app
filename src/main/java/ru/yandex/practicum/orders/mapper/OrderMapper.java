package ru.yandex.practicum.orders.mapper;

import ru.yandex.practicum.items.dto.ItemShortDto;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.model.OrderItem;

import java.util.List;
import java.util.Map;

public class OrderMapper {

    public static OrderDto toOrderDto(
            Long orderId,
            Long totalSum,
            List<OrderItem> orderItems,
            Map<Long, Item> itemsById
    ) {
        List<ItemShortDto> items = orderItems.stream()
                .map(orderItem -> {
                    Item item = itemsById.get(orderItem.getItemId());

                    return ItemShortDto.builder()
                            .id(item.getId())
                            .title(item.getTitle())
                            .price(orderItem.getPrice())
                            .count(orderItem.getCount())
                            .build();
                })
                .toList();

        return OrderDto.builder()
                .id(orderId)
                .totalSum(totalSum)
                .items(items)
                .build();
    }
}