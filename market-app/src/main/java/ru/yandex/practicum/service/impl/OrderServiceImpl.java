package ru.yandex.practicum.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.payment.PaymentRequestDto;
import ru.yandex.practicum.dto.payment.PaymentStatus;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.mapper.OrderMapper;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderItem;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.OrderItemRepository;
import ru.yandex.practicum.repository.OrderRepository;
import ru.yandex.practicum.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final PaymentClient paymentClient;
    private final ItemCacheService cacheService;

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

                    return Flux.fromIterable(cartItems)
                            .flatMap(cartItem -> getItemCard(cartItem.getItemId())
                                    .map(itemCard -> Tuples.of(cartItem, itemCard)))
                            .collectList()
                            .flatMap(cartItemsWithCards -> {
                                long totalSum = cartItemsWithCards.stream()
                                        .mapToLong(tuple -> {
                                            CartItem cartItem = tuple.getT1();
                                            ItemCardCacheDto itemCard = tuple.getT2();

                                            return itemCard.getPrice() * cartItem.getCount();
                                        })
                                        .sum();

                                PaymentRequestDto paymentRequest = new PaymentRequestDto(
                                        null,
                                        BigDecimal.valueOf(totalSum),
                                        "RUB"
                                );

                                return paymentClient.makePayment(paymentRequest)
                                        .flatMap(paymentResponse -> {
                                            if (paymentResponse.getStatus() != PaymentStatus.PAID) {
                                                return Mono.error(new BadRequestException(
                                                        paymentResponse.getMessage()
                                                ));
                                            }

                                            Order order = new Order(
                                                    null,
                                                    totalSum,
                                                    LocalDateTime.now()
                                            );

                                            return orderRepository.save(order)
                                                    .flatMap(savedOrder -> {
                                                        List<OrderItem> orderItems = cartItemsWithCards.stream()
                                                                .map(tuple -> {
                                                                    CartItem cartItem = tuple.getT1();
                                                                    ItemCardCacheDto itemCard = tuple.getT2();

                                                                    return new OrderItem(
                                                                            null,
                                                                            savedOrder.getId(),
                                                                            itemCard.getId(),
                                                                            cartItem.getCount(),
                                                                            itemCard.getPrice()
                                                                    );
                                                                })
                                                                .toList();

                                                        return orderItemRepository.saveAll(orderItems)
                                                                .then(cartItemRepository.deleteAll())
                                                                .thenReturn(savedOrder.getId());
                                                    });
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

    private Mono<ItemCardCacheDto> getItemCard(long itemId) {
        return cacheService.getItemCard(itemId)
                .switchIfEmpty(Mono.defer(() ->
                        itemRepository.findById(itemId)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                        "Item with id = %d not found".formatted(itemId)
                                                )))
                                .map(ItemMapper::toItemCardCacheDto)
                                .flatMap(item -> cacheService.saveItemCard(item)
                                        .thenReturn(item))
                                ));
    }
}
