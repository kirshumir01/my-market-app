package ru.yandex.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.cart.CartViewDto;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.exception.UnauthorizedException;
import ru.yandex.practicum.mapper.CartMapper;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.CartService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String DEFAULT_CURRENCY = "RUB";

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final ItemCacheService cacheService;
    private final PaymentClient paymentClient;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Mono<CartViewDto> getCartView(String username, boolean paymentError) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UnauthorizedException("User is not authenticated")))
                .flatMap(user -> getCart(username)
                        .flatMap(cart -> paymentClient.getBalance(user.getId())
                                .map(balance -> CartMapper.toCartViewDto(
                                        cart,
                                        balance.getBalance(),
                                        balance.getCurrency(),
                                        paymentError,
                                        false
                                ))
                                .onErrorResume(ex -> {
                                    if (ex instanceof WebClientResponseException e) {
                                        log.warn(
                                                "Payment service returned {} {}",
                                                e.getStatusCode(),
                                                e.getResponseBodyAsString()
                                        );
                                    } else {
                                        log.warn("Payment service is unavailable", ex);
                                    }

                                    return Mono.just(CartMapper.toCartViewDto(
                                            cart,
                                            null,
                                            DEFAULT_CURRENCY,
                                            paymentError,
                                            true
                                    ));
                                })));
    }

    public Mono<CartDto> getCart(String username) {
        return getUser(username)
                .flatMapMany(user -> cartItemRepository.findAllByUserId(user.getId()))
                .flatMap(this::toItemDto)
                .collectList()
                .map(this::toCartDto);
    }

    private Mono<ItemDto> toItemDto(CartItem cartItem) {
        return cacheService.getItemCardCached(cartItem.getItemId())
                .map(itemCard -> {
                    ItemDto itemDto = ItemMapper.toItemDto(itemCard);
                    itemDto.setCount(cartItem.getCount());
                    return itemDto;
                });
    }

    private CartDto toCartDto(List<ItemDto> items) {
        long total = items.stream()
                .mapToLong(item -> item.getPrice() * item.getCount())
                .sum();

        return CartMapper.toCartDto(items, total);
    }

    @Override
    @Transactional
    public Mono<Void> changeItemsCount(String username, long itemId, CartAction action) {
        return getUser(username)
                .flatMap(user -> switch (action) {
                    case PLUS -> addItem(user.getId(), itemId);
                    case MINUS -> decreaseItem(user.getId(), itemId);
                    case DELETE -> deleteItem(user.getId(), itemId);
                });
    }

    private Mono<Void> addItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .flatMap(cartItem -> {
                    cartItem.setCount(cartItem.getCount() + 1);
                    return cartItemRepository.save(cartItem);
                })
                .switchIfEmpty(
                        itemRepository.findById(itemId)
                                .switchIfEmpty(Mono.error(
                                        new NotFoundException("Item with id = %d not found".formatted(itemId))
                                ))
                                .flatMap(item -> cartItemRepository.save(
                                        new CartItem(null, userId, item.getId(), 1)
                                ))
                )
                .then();
    }

    private Mono<Void> decreaseItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Cart item with item id = %d not found".formatted(itemId))
                ))
                .flatMap(cartItem -> {
                    if (cartItem.getCount() <= 1) {
                        return cartItemRepository.delete(cartItem);
                    }

                    cartItem.setCount(cartItem.getCount() - 1);
                    return cartItemRepository.save(cartItem).then();
                });
    }

    private Mono<Void> deleteItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Cart item with item id = %d not found".formatted(itemId))
                ))
                .flatMap(cartItemRepository::delete);
    }

    private Mono<User> getUser(String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new AccessDeniedException("User is not authenticated"));
        }

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("User with username = %s not found".formatted(username))
                ));
    }
}