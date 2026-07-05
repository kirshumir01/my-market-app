package ru.yandex.practicum.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.model.Order;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    @Query("""
            SELECT *
            FROM orders
            WHERE user_id = :userId
            ORDER BY id ASC
            """)
    Flux<Order> findAllByUserIdOrderByIdAsc(long userId);

    @Query("""
            SELECT *
            FROM orders
            WHERE id = :orderId AND user_id = :userId
            """)
    Mono<Order> findByIdAndUserId(long orderId, long userId);
}