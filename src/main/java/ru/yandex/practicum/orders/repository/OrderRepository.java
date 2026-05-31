package ru.yandex.practicum.orders.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.orders.model.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT DISTINCT o
            FROM Order AS o
            JOIN FETCH o.items AS oi
            JOIN FETCH oi.item
            ORDER BY o.id ASC
            """)
    List<Order> findAllWithItems();

    @Query("""
            SELECT DISTINCT o
            FROM Order AS o
            JOIN FETCH o.items AS oi
            JOIN FETCH oi.item
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdWithItems(@Param("orderId") long orderId);
}
