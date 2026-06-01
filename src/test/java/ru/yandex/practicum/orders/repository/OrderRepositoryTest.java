package ru.yandex.practicum.orders.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.orders.model.Order;
import ru.yandex.practicum.orders.model.OrderItem;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/clear.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void findAllWithItems_shouldReturnOrdersWithItems() {
        List<Order> result = orderRepository.findAllWithItems();

        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(1L, 2L);

        Order foundOrder1 = result.stream()
                .filter(order -> order.getId().equals(1L))
                .findFirst()
                .orElseThrow();

        Order foundOrder2 = result.stream()
                .filter(order -> order.getId().equals(2L))
                .findFirst()
                .orElseThrow();

        assertThat(foundOrder1.getItems()).hasSize(2);
        assertThat(foundOrder1.getItems())
                .extracting(orderItem -> orderItem.getItem().getTitle())
                .containsExactlyInAnyOrder(
                        "Test item_1 title",
                        "Test item_2 title"
                );

        assertThat(foundOrder2.getItems()).hasSize(1);
        assertThat(foundOrder2.getItems())
                .extracting(orderItem -> orderItem.getItem().getTitle())
                .containsExactly("Test item_3 title");
    }

    @Test
    void findByIdWithItems_shouldReturnOrderWithItems() {
        Optional<Order> result = orderRepository.findByIdWithItems(1L);

        assertThat(result).isPresent();

        Order foundOrder = result.get();

        assertThat(foundOrder.getId()).isEqualTo(1L);
        assertThat(foundOrder.getTotalSum()).isEqualTo(999L * 2 + 2999L);
        assertThat(foundOrder.getItems()).hasSize(2);

        assertThat(foundOrder.getItems())
                .extracting(OrderItem::getCount)
                .containsExactlyInAnyOrder(2, 1);

        assertThat(foundOrder.getItems())
                .extracting(orderItem -> orderItem.getItem().getId())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void findByIdWithItems_shouldReturnEmptyWhenOrderDoesNotExist() {
        Optional<Order> result = orderRepository.findByIdWithItems(999L);

        assertThat(result).isEmpty();
    }
}