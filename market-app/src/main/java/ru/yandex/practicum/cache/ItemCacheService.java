package ru.yandex.practicum.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;

import java.time.Duration;

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

    public Mono<Boolean> saveItemCard(ItemCardCacheDto dto) {
        return redisTemplate.opsForValue()
                .set("product:card:" + dto.getId(), dto, cacheTtl);
    }

    public Mono<Long> evictItemLists() {
        return redisTemplate.keys("item:list:*")
                .flatMap(redisTemplate::delete)
                .count();
    }
}