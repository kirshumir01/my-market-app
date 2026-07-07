package ru.yandex.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.item.ItemRequest;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.service.ItemService;
import ru.yandex.practicum.service.UserService;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemCacheService cacheService;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemDto> getItem(String username, long itemId) {
        Mono<Integer> countMono = username == null || username.isBlank()
                ? Mono.just(0)
                : userService.getRequiredUserId(username)
                .flatMap(userId -> cartItemRepository.findByUserIdAndItemId(userId, itemId))
                .map(CartItem::getCount)
                .defaultIfEmpty(0);

        return cacheService.getItemCardCached(itemId)
                .zipWith(countMono)
                .map(tuple -> {
                    ItemDto itemDto = ItemMapper.toItemDto(tuple.getT1());
                    itemDto.setCount(tuple.getT2());
                    return itemDto;
                });
    }

    @Override
    @Transactional
    public Mono<ItemDto> createItem(ItemRequest request) {
        Item item = new Item(
                null,
                request.getTitle(),
                request.getDescription(),
                request.getImgPath(),
                request.getPrice()
        );

        return itemRepository.save(item)
                .flatMap(savedItem ->
                        cacheService.evictItemLists()
                                .thenReturn(ItemMapper.toItemDto(savedItem))
                );
    }
}