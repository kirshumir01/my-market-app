package ru.yandex.practicum.cart.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.items.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/clear.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CartItemRepositoryTest {

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ItemRepository itemRepository;

    @Test
    void findAllByItemIdIn_shouldReturnItems() {
        List<Long> itemIds = List.of(1L, 2L);

        List<CartItem> result = cartItemRepository.findAllByItemIdIn(itemIds);

        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(cartItem -> cartItem.getItem().getId())
                .containsExactlyInAnyOrder(1L, 2L);

        assertThat(result)
                .extracting(CartItem::getCount)
                .containsExactlyInAnyOrder(2, 5);
    }

    @Test
    void findAllByItemIdIn_shouldNotReturnItemsWhenIdsDoNotExist() {
        List<CartItem> result = cartItemRepository.findAllByItemIdIn(List.of(999L, 1000L));

        assertThat(result).isEmpty();
    }

    @Test
    void findByItemId_shouldReturnItem() {
        Optional<CartItem> result = cartItemRepository.findByItemId(1L);

        assertThat(result).isNotNull();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getItem().getId()).isEqualTo(1L);
        assertThat(result.get().getCount()).isEqualTo(2);
    }

    @Test
    void findByItemId_shouldReturnEmptyOptionalWhenItemDoesNotExistInCart() {
        Optional<CartItem> result = cartItemRepository.findByItemId(3L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllWithItems_shouldReturnCartItemsWithItems() {
        List<CartItem> result = cartItemRepository.findAllWithItems();

        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(cartItem -> cartItem.getItem().getTitle())
                .containsExactlyInAnyOrder(
                        "Test item_1 title",
                        "Test item_2 title"
                );

        assertThat(result)
                .extracting(CartItem::getCount)
                .containsExactlyInAnyOrder(2, 5);
    }
}