package ru.yandex.practicum.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.OrderItem;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class OrderItemRepositoryTest extends TestDataConfiguration {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("findAllByOrderIdOrderByIdAsc(orderId) -> returns all order items")
    void findAllByOrderIdOrderByIdAsc_shouldReturnOrderItems() {
        List<OrderItem> result = orderItemRepository
                .findAllByOrderIdOrderByIdAsc(1L)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(OrderItem::getId)
                .containsExactly(1L, 2L);

        assertThat(result)
                .extracting(OrderItem::getOrderId)
                .containsOnly(1L);

        assertThat(result)
                .extracting(OrderItem::getItemId)
                .containsExactly(1L, 2L);

        assertThat(result)
                .extracting(OrderItem::getCount)
                .containsExactly(2, 1);

        assertThat(result)
                .extracting(OrderItem::getPrice)
                .containsExactly(999L, 2999L);
    }

    @Test
    @DisplayName("findAllByOrderIdOrderByIdAsc(orderId) -> returns single order item")
    void findAllByOrderIdOrderByIdAsc_shouldReturnSingleOrderItem() {
        List<OrderItem> result = orderItemRepository
                .findAllByOrderIdOrderByIdAsc(2L)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        OrderItem orderItem = result.getFirst();

        assertThat(orderItem.getId()).isEqualTo(3L);
        assertThat(orderItem.getOrderId()).isEqualTo(2L);
        assertThat(orderItem.getItemId()).isEqualTo(3L);
        assertThat(orderItem.getCount()).isEqualTo(3);
        assertThat(orderItem.getPrice()).isEqualTo(7999L);
    }

    @Test
    @DisplayName("findAllByOrderIdOrderByIdAsc(orderId) -> returns empty when order does not exist")
    void findAllByOrderIdOrderByIdAsc_shouldReturnEmptyWhenOrderDoesNotExist() {
        List<OrderItem> result = orderItemRepository
                .findAllByOrderIdOrderByIdAsc(999L)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}