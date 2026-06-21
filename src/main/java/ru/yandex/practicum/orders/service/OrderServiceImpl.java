package ru.yandex.practicum.orders.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.mapper.OrderMapper;
import ru.yandex.practicum.orders.model.Order;
import ru.yandex.practicum.orders.model.OrderItem;
import ru.yandex.practicum.orders.repository.OrderItemRepository;
import ru.yandex.practicum.orders.repository.OrderRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderDto> getOrders() {
        return orderRepository.findAllOrders()
                .flatMap(this::toOrderDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OrderDto> getOrder(long orderId) {
        return orderRepository.findOrderById(orderId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Order with id = %d not found".formatted(orderId))
                ))
                .flatMap(this::toOrderDto);
    }

    @Override
    @Transactional
    public Mono<Long> createOrderFromCart() {
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new BadRequestException("Cart is empty"));
                    }

                            List<Long> itemIds = cartItems.stream()
                                    .map(CartItem::getItemId)
                                    .toList();

                            return itemRepository.findAllById(itemIds)
                                    .collectMap(Item::getId)
                                    .flatMap(itemsById -> {
                                        long totalSum = cartItems.stream()
                                                .mapToLong(cartItem -> {
                                                    Item item = itemsById.get(cartItem.getItemId());
                                                    return item.getPrice() * cartItem.getCount();
                                                })
                                                .sum();

                                                return orderRepository.save(new Order(null, totalSum))
                                                        .flatMap(savedOrder -> {
                                                            List<OrderItem> orderItems = cartItems.stream()
                                                                    .map(cartItem -> {
                                                                        Item item = itemsById.get(cartItem.getItemId());

                                                                        return new OrderItem(
                                                                                null,
                                                                                savedOrder.getId(),
                                                                                item.getId(),
                                                                                cartItem.getCount(),
                                                                                item.getPrice()
                                                                        );
                                                                    })
                                                                    .toList();

                                                            return orderItemRepository.saveAll(orderItems)
                                                                    .then(cartItemRepository.deleteAll())
                                                                    .thenReturn(savedOrder.getId());
                                                        });
                                    });
                });
    }

    private Mono<OrderDto> toOrderDto(Order order) {
        return orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId())
                .collectList()
                .flatMap(orderItems -> {
                    List<Long> itemIds = orderItems.stream()
                            .map(OrderItem::getItemId)
                            .toList();

                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .map(itemsById -> OrderMapper.toOrderDto(
                                    order.getId(),
                                    order.getTotalSum(),
                                    orderItems,
                                    itemsById
                            ));
                });
    }
}
