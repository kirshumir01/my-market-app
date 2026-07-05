package ru.yandex.practicum.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.dto.cart.CartItemWithCard;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.payment.PaymentRequestDto;
import ru.yandex.practicum.dto.payment.PaymentStatus;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.OrderMapper;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;
import ru.yandex.practicum.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String DEFAULT_CURRENCY = "RUB";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final PaymentClient paymentClient;
    private final ItemCacheService cacheService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderDto> getOrders(String username) {
        return getUserId(username)
                .flatMapMany(userId -> orderRepository.findAllByUserIdOrderByIdAsc(userId)
                        .flatMap(this::toOrderDto));
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OrderDto> getOrder(String username, long orderId) {
        return getUserId(username)
                .flatMap(userId -> orderRepository.findByIdAndUserId(orderId, userId))
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Order with id = %d not found".formatted(orderId))
                ))
                .flatMap(this::toOrderDto);
    }

    @Override
    @Transactional
    public Mono<Long> createOrderFromCart(String username) {
        return getUserId(username)
                .flatMap(userId -> getCartItemsWithCards(userId)
                        .flatMap(items -> makePayment(userId, items)
                                .then(Mono.defer(() -> saveOrder(userId, items)))));
    }

    private Mono<Long> getUserId(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("User with username = %s not found".formatted(username))
                ))
                .map(User::getId);
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

    private Mono<List<CartItemWithCard>> getCartItemsWithCards(long userId) {
        return cartItemRepository.findAllByUserId(userId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new BadRequestException("Cart is empty"));
                    }

                    return Flux.fromIterable(cartItems)
                            .flatMap(this::toCartItemWithCard)
                            .collectList();
                });
    }

    private Mono<CartItemWithCard> toCartItemWithCard(CartItem cartItem) {
        return cacheService.getItemCardCached(cartItem.getItemId())
                .map(itemCard -> new CartItemWithCard(cartItem, itemCard));
    }

    private Mono<Void> makePayment(
            Long userId,
            List<CartItemWithCard> items
    ) {
        long totalSum = calculateTotalSum(items);

        PaymentRequestDto paymentRequest = new PaymentRequestDto(
                null,
                userId,
                BigDecimal.valueOf(totalSum),
                DEFAULT_CURRENCY
        );

        return paymentClient.makePayment(paymentRequest)
                .flatMap(paymentResponse -> {
                    if (paymentResponse.getStatus() != PaymentStatus.PAID) {
                        return Mono.error(new BadRequestException(paymentResponse.getMessage()));
                    }

                    return Mono.empty();
                });
    }

    private Mono<Long> saveOrder(long userId, List<CartItemWithCard> items) {
        long totalSum = calculateTotalSum(items);

        Order order = new Order(
                null,
                userId,
                totalSum,
                LocalDateTime.now()
        );

        return orderRepository.save(order)
                .flatMap(savedOrder -> saveOrderItems(savedOrder, items)
                        .then(clearCart(userId))
                        .thenReturn(savedOrder.getId()));
    }

    private Mono<Void> saveOrderItems(Order order, List<CartItemWithCard> items) {
        List<OrderItem> orderItems = items.stream()
                .map(item -> toOrderItem(order, item))
                .toList();

        return orderItemRepository.saveAll(orderItems).then();
    }

    private OrderItem toOrderItem(Order order, CartItemWithCard item) {
        return new OrderItem(
                null,
                order.getId(),
                item.getItemCard().getId(),
                item.getCartItem().getCount(),
                item.getItemCard().getPrice()
        );
    }

    private Mono<Void> clearCart(long userId) {
        return cartItemRepository.deleteAllByUserId(userId);
    }

    private long calculateTotalSum(List<CartItemWithCard> items) {
        return items.stream()
                .mapToLong(item -> item.getItemCard().getPrice() * item.getCartItem().getCount())
                .sum();
    }
}
