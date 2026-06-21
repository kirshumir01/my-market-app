package ru.yandex.practicum.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.mapper.CartMapper;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.mapper.ItemMapper;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public Mono<CartDto> getCart() {
        return cartItemRepository.findAll()
                .collectList()
                .flatMap(cartItems -> {
                    List<Long> itemIds = cartItems.stream()
                            .map(CartItem::getItemId)
                            .toList();

                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .map(itemsById -> {
                                List<ItemDto> items = cartItems.stream()
                                        .map(cartItem -> {
                                            Item item = itemsById.get(cartItem.getItemId());
                                            ItemDto itemDto = ItemMapper.toItemDto(item);
                                            itemDto.setCount(cartItem.getCount());
                                            return itemDto;
                                        }).toList();

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
}
