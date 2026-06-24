package ru.yandex.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.CartMapper;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.service.CartService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final ItemCacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public Mono<CartDto> getCart() {
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.just(CartMapper.toCartDto(List.of(), 0));
                    }

                    return Flux.fromIterable(cartItems)
                            .flatMap(cartItem ->
                                    getItemCardFromCache(cartItem.getItemId())
                                            .map(itemCard -> {
                                                ItemDto itemDto = ItemMapper.toItemDto(itemCard);
                                                itemDto.setCount(cartItem.getCount());
                                                return itemDto;
                                            })
                            )
                            .collectList()
                            .map(items -> {
                                long total = items.stream()
                                        .mapToLong(item -> item.getPrice() * item.getCount())
                                        .sum();

                                return CartMapper.toCartDto(items, total);
                            });
                });
    }

    @Override
    @Transactional
    public Mono<Void> changeItemsCount(long itemId, CartAction action) {
        return switch (action) {
            case PLUS -> addItem(itemId);
            case MINUS -> decreaseItem(itemId);
            case DELETE -> deleteItem(itemId);
        };
    }

    private Mono<Void> addItem(long itemId) {
        return cartItemRepository.findByItemId(itemId)
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
                                        new CartItem(null, item.getId(), 1)
                                ))
                )
                .then();
    }

    private Mono<Void> decreaseItem(long itemId) {
        return cartItemRepository.findByItemId(itemId)
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

    private Mono<Void> deleteItem(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("Cart item with item id = %d not found".formatted(itemId))
                ))
                .flatMap(cartItemRepository::delete);
    }

    private Mono<ItemCardCacheDto> getItemCardFromCache(long itemId) {
        return cacheService.getItemCard(itemId)
                .switchIfEmpty(Mono.defer(() ->
                        itemRepository.findById(itemId)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                        "Item with id = %d not found".formatted(itemId)
                                )))
                                .map(ItemMapper::toItemCardCacheDto)
                                .flatMap(itemCard -> cacheService.saveItemCard(itemCard)
                                        .thenReturn(itemCard))
                ));
    }
}
