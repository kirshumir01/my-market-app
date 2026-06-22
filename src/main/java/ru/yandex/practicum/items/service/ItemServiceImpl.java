package ru.yandex.practicum.items.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.mapper.ItemMapper;
import ru.yandex.practicum.items.repository.ItemRepository;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemDto> getItem(long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(
                                "Item with id = %d not found".formatted(itemId)
                        )))
                .flatMap(item -> cartItemRepository.findByItemId(item.getId())
                        .map(CartItem::getCount)
                        .defaultIfEmpty(0)
                        .map(count -> {
                            ItemDto itemDto = ItemMapper.toItemDto(item);
                            itemDto.setCount(count);
                            return itemDto;
                        })
                );
    }
}
