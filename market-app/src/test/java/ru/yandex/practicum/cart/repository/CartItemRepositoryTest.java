package ru.yandex.practicum.cart.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.repository.CartItemRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemRepositoryTest extends TestDataConfiguration {

    @Autowired
    CartItemRepository cartItemRepository;

    @Test
    @DisplayName("findAllByItemIdIn(itemIds) -> returns cart items")
    void findAllByItemIdIn_shouldReturnItems() {
        List<Long> itemIds = List.of(1L, 2L);

        List<CartItem> result = cartItemRepository.findAllByItemIdIn(itemIds)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(CartItem::getItemId)
                .containsExactlyInAnyOrder(1L, 2L);

        assertThat(result)
                .extracting(CartItem::getCount)
                .containsExactlyInAnyOrder(2, 5);
    }

    @Test
    @DisplayName("findAllByItemIdIn(itemIds) -> returns empty when items do not exist")
    void findAllByItemIdIn_shouldReturnEmptyWhenItemsDoNotExist() {
        List<CartItem> result = cartItemRepository.findAllByItemIdIn(List.of(999L, 1000L))
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByItemId(itemId) -> returns cart item")
    void findByItemId_shouldReturnItem() {
        CartItem result = cartItemRepository.findByItemId(1L)
                .block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItemId()).isEqualTo(1L);
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByItemId(itemId) -> returns empty when item is not in cart")
    void findByItemId_shouldReturnEmptyWhenItemDoesNotExistInCart() {
        CartItem result = cartItemRepository.findByItemId(3L)
                .block();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findAll() -> returns all cart items")
    void findAll_shouldReturnAllCartItems() {
        List<CartItem> result = cartItemRepository.findAll()
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(CartItem::getItemId)
                .containsExactlyInAnyOrder(1L, 2L);

        assertThat(result)
                .extracting(CartItem::getCount)
                .containsExactlyInAnyOrder(2, 5);
    }
}