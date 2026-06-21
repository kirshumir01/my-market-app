package ru.yandex.practicum.orders.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.orders.model.Order;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class OrderRepositoryTest extends TestDataConfiguration {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void findAllOrders_shouldReturnOrders() {
        List<Order> result = orderRepository.findAllOrders()
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(1L, 2L);

        assertThat(result)
                .extracting(Order::getTotalSum)
                .containsExactlyInAnyOrder(
                        999L * 2 + 2999L,
                        7999L * 3
                );
    }

    @Test
    void findOrderById_shouldReturnOrder() {
        Order result = orderRepository.findOrderById(1L)
                .block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTotalSum()).isEqualTo(999L * 2 + 2999L);
    }

    @Test
    void findOrderById_shouldReturnEmptyWhenOrderDoesNotExist() {
        Order result = orderRepository.findOrderById(999L)
                .block();

        assertThat(result).isNull();
    }
}