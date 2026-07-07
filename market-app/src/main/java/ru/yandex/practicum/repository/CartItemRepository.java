package ru.yandex.practicum.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.model.CartItem;

import java.util.List;

@Repository
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findAllByUserId(long userId);

    Flux<CartItem> findAllByUserIdAndItemIdIn(long userId, List<Long> itemIds);

    Mono<CartItem> findByUserIdAndItemId(long userId, long itemId);

    Mono<Void> deleteAllByUserId(long userId);
}