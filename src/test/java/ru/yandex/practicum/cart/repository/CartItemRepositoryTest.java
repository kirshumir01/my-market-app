package ru.yandex.practicum.cart.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.config.TestDataConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemRepositoryTest extends TestDataConfiguration {

    @Autowired
    CartItemRepository cartItemRepository;

    @Test
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
    void findAllByItemIdIn_shouldNotReturnItemsWhenIdsDoNotExist() {
        List<CartItem> result = cartItemRepository.findAllByItemIdIn(List.of(999L, 1000L))
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void findByItemId_shouldReturnItem() {
        CartItem result = cartItemRepository.findByItemId(1L)
                .block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItemId()).isEqualTo(1L);
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    void findByItemId_shouldReturnEmptyWhenItemDoesNotExistInCart() {
        CartItem result = cartItemRepository.findByItemId(3L)
                .block();

        assertThat(result).isNull();
    }

    @Test
    void findAll_shouldReturnCartItems() {
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