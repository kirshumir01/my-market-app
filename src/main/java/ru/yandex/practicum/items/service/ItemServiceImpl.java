package ru.yandex.practicum.items.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.mapper.ItemMapper;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItem(long itemId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(String.format("Item with id = %d not found", itemId)));

        int count = cartItemRepository.findByItemId(item.getId()).map(CartItem::getCount).orElse(0);

        ItemDto itemDto = ItemMapper.toItemDto(item);
        itemDto.setCount(count);

        return itemDto;
    }
}
