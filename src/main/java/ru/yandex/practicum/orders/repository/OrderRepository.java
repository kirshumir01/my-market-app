package ru.yandex.practicum.orders.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.orders.model.Order;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    @Query("""
            SELECT *
            FROM orders
            ORDER BY id ASC
            """)
    Flux<Order> findAllOrders();

    @Query("""
            SELECT *
            FROM orders
            WHERE id = :orderId
            """)
    Mono<Order> findOrderById(long orderId);
}
