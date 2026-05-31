package ru.yandex.practicum.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public CartDto getCart() {
        List<CartItem> cartItemList = cartItemRepository.findAllWithItems();
        List<ItemDto> itemDtoList = new ArrayList<>();

        cartItemList
                .forEach(cartItem -> {
                    ItemDto itemDto = ItemMapper.itemDto(cartItem);
                    itemDtoList.add(itemDto);
                });

        long total = cartItemList.stream()
                .mapToLong(cartItem -> cartItem.getItem().getPrice() * cartItem.getCount())
                .sum();

        return CartMapper.toCartDto(itemDtoList, total);
    }

    @Override
    @Transactional
    public void changeItemsCount(long itemId, CartAction action) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(String.format("Item with id = %d not found", itemId)));

        CartItem cartItem = cartItemRepository.findByItemId(itemId).orElse(null);

        switch (action) {
            case PLUS -> {
                if (cartItem != null) {
                    cartItem.setCount(cartItem.getCount() + 1);
                    cartItemRepository.save(cartItem);
                } else {
                    CartItem newCartItem = CartItem.builder()
                            .item(item)
                            .count(1)
                            .build();
                    cartItemRepository.save(newCartItem);
                }
            }
            case MINUS -> {
                if (cartItem == null) return;

                if (cartItem.getCount() > 1) {
                    cartItem.setCount(cartItem.getCount() - 1);
                    cartItemRepository.save(cartItem);
                } else {
                    cartItemRepository.delete(cartItem);
                }
            }
            case DELETE -> {
                if (cartItem != null) {
                    cartItemRepository.delete(cartItem);
                }
            }

        }
    }
}
