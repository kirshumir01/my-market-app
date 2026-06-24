package ru.yandex.practicum.orders.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.OrderItem;
import ru.yandex.practicum.repository.OrderItemRepository;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class OrderItemRepositoryTest extends TestDataConfiguration {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void findAllByOrderIdOrderByIdAsc_shouldReturnOrderItems() {
        List<OrderItem> result = orderItemRepository.findAllByOrderIdOrderByIdAsc(1L)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(OrderItem::getItemId)
                .containsExactlyInAnyOrder(1L, 2L);

        assertThat(result)
                .extracting(OrderItem::getCount)
                .containsExactlyInAnyOrder(2, 1);

        assertThat(result)
                .extracting(OrderItem::getPrice)
                .containsExactlyInAnyOrder(999L, 2999L);
    }

    @Test
    void findAllByOrderIdOrderByIdAsc_shouldReturnEmptyWhenOrderDoesNotExist() {
        List<OrderItem> result = orderItemRepository.findAllByOrderIdOrderByIdAsc(999L)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}