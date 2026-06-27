package ru.yandex.practicum.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.repository.ItemRepository;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ItemCacheService {

    @Value("${cache.item.ttl}")
    private Duration cacheTtl;

    private final ItemRepository itemRepository;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Mono<ItemCardCacheDto> getItemCard(Long id) {
        return redisTemplate.opsForValue()
                .get(CacheKeys.itemCard(id))
                .map(value -> objectMapper.convertValue(value, ItemCardCacheDto.class));
    }

    public Mono<Boolean> saveItemCard(ItemCardCacheDto dto) {
        return redisTemplate.opsForValue()
                .set(CacheKeys.itemCard(dto.getId()), dto, cacheTtl);
    }

    public Mono<Long> evictItemLists() {
        return redisTemplate.keys(CacheKeys.itemListPattern())
                .flatMap(redisTemplate::delete)
                .count();
    }

    public Mono<ItemCardCacheDto> getItemCardCached(Long itemId) {
        return getItemCard(itemId)
                .switchIfEmpty(Mono.defer(() -> itemRepository.findById(itemId)
                        .switchIfEmpty(Mono.error(new NotFoundException(
                                "Item with id = %d not found".formatted(itemId)
                        )))
                        .map(ItemMapper::toItemCardCacheDto)
                        .flatMap(itemCard -> saveItemCard(itemCard).thenReturn(itemCard))));
    }
}