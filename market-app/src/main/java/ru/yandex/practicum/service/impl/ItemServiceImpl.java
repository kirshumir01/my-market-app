package ru.yandex.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.item.ItemRequest;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.ItemService;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {


    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemCacheService cacheService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemDto> getItem(String username, long itemId) {
        return cacheService.getItemCardCached(itemId)
                .flatMap(itemCard -> {
                    Mono<Integer> countMono = username == null || username.isBlank()
                            ? Mono.just(0)
                            : getUserId(username)
                            .flatMap(userId -> cartItemRepository.findByUserIdAndItemId(userId, itemId))
                            .map(CartItem::getCount)
                            .defaultIfEmpty(0);

                    return countMono.map(count -> {
                        ItemDto itemDto = ItemMapper.toItemDto(itemCard);
                        itemDto.setCount(count);
                        return itemDto;
                    });
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

    private Mono<Long> getUserId(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("User with username = %s not found".formatted(username))
                ))
                .map(User::getId);
    }
}