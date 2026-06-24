package ru.yandex.practicum.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.cache.ItemListCacheDto;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemCacheService {

    @Value("${cache.product.ttl}")
    private Duration cacheTtl;

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Mono<ItemCardCacheDto> getItemCard(Long id) {
        return redisTemplate.opsForValue()
                .get("product:card:" + id)
                .map(value -> objectMapper.convertValue(value, ItemCardCacheDto.class));
    }

    public Mono<List<ItemListCacheDto>> getItemList() {
        return redisTemplate.opsForValue()
                .get("product:list")
                .map(value -> objectMapper.convertValue(
                        value,
                        new TypeReference<List<ItemListCacheDto>>() {}
                ));
    }

    public Mono<Boolean> saveItemCard(ItemCardCacheDto dto) {
        return redisTemplate.opsForValue()
                .set("product:card:" + dto.getId(), dto, cacheTtl);
    }

    public Mono<Boolean> saveItemList(List<ItemListCacheDto> products) {
        return redisTemplate.opsForValue()
                .set("product:list", products, cacheTtl);
    }

    public Mono<Long> evictItemLists() {
        return redisTemplate.keys("item:list:*")
                .flatMap(redisTemplate::delete)
                .count();
    }
}