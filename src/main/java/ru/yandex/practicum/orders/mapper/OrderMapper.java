package ru.yandex.practicum.orders.mapper;

import ru.yandex.practicum.items.dto.ItemShortDto;
import ru.yandex.practicum.items.mapper.ItemMapper;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderMapper {

    public static List<OrderDto> toOrderDtoList(List<Order> orders) {
        List<OrderDto> orderDtoList = new ArrayList<>();

        orders.forEach(order -> {
            List<ItemShortDto> itemShortDtoList = new ArrayList<>();

            order.getItems().forEach(orderItem -> {
                Item item = orderItem.getItem();

                ItemShortDto itemShortDto = ItemMapper.toItemShortDto(item);
                itemShortDto.setPrice(orderItem.getPrice());
                itemShortDto.setCount(orderItem.getCount());

                itemShortDtoList.add(itemShortDto);
            });

            OrderDto orderDto = OrderDto.builder()
                    .id(order.getId())
                    .items(itemShortDtoList)
                    .totalSum(order.getTotalSum())
                    .build();

            orderDtoList.add(orderDto);
        });

        return orderDtoList;
    }

    public static OrderDto toOrderDto(Order order) {
        List<ItemShortDto> itemShortDtoList = new ArrayList<>();

        order.getItems().forEach(orderItem -> {
            Item item = orderItem.getItem();
            ItemShortDto itemShortDto = ItemMapper.toItemShortDto(item);
            itemShortDto.setPrice(orderItem.getPrice());
            itemShortDto.setCount(orderItem.getCount());
            itemShortDtoList.add(itemShortDto);
        });

        return OrderDto.builder()
                .id(order.getId())
                .items(itemShortDtoList)
                .totalSum(order.getTotalSum())
                .build();
    }
}
