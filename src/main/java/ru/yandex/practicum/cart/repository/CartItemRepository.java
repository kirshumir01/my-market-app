package ru.yandex.practicum.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.cart.model.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByItemIdIn(List<Long> itemIds);

    Optional<CartItem> findByItemId(long itemId);

    @Query("""
            SELECT ci
            FROM CartItem AS ci
            JOIN FETCH ci.item
            ORDER BY ci.item.id DESC
            """)
    List<CartItem> findAllWithItems();

    @Query("""
       SELECT ci
       FROM CartItem ci
       JOIN FETCH ci.item
       WHERE ci.item.id = :itemId
       """)
    Optional<CartItem> findByItemIdWithItem(@Param("itemId") long itemId);
}
