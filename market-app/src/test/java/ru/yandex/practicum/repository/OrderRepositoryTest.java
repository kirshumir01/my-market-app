package ru.yandex.practicum.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.Order;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class OrderRepositoryTest extends TestDataConfiguration {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;

    @Autowired
    OrderRepository orderRepository;

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("findAllByUserIdOrderByIdAsc(userId) -> returns user's orders")
    void findAllByUserIdOrderByIdAsc_shouldReturnUserOrders() {
        List<Order> result = orderRepository.findAllByUserIdOrderByIdAsc(USER_ID)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Order::getId)
                .containsExactly(1L, 2L);

        assertThat(result)
                .extracting(Order::getUserId)
                .containsOnly(USER_ID);

        assertThat(result)
                .extracting(Order::getTotalSum)
                .containsExactly(
                        999L * 2 + 2999L,
                        7999L * 3
                );
    }

    @Test
    @DisplayName("findAllByUserIdOrderByIdAsc(userId) -> returns empty when user has no orders")
    void findAllByUserIdOrderByIdAsc_shouldReturnEmptyWhenUserHasNoOrders() {
        List<Order> result = orderRepository.findAllByUserIdOrderByIdAsc(OTHER_USER_ID)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndUserId(orderId, userId) -> returns order when it belongs to user")
    void findByIdAndUserId_shouldReturnOrder() {
        Order result = orderRepository.findByIdAndUserId(1L, USER_ID)
                .block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTotalSum()).isEqualTo(999L * 2 + 2999L);
    }

    @Test
    @DisplayName("findByIdAndUserId(orderId, userId) -> returns empty when order belongs to another user")
    void findByIdAndUserId_shouldReturnEmptyWhenOrderBelongsToAnotherUser() {
        Order result = orderRepository.findByIdAndUserId(1L, OTHER_USER_ID)
                .block();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByIdAndUserId(orderId, userId) -> returns empty when order does not exist")
    void findByIdAndUserId_shouldReturnEmptyWhenOrderDoesNotExist() {
        Order result = orderRepository.findByIdAndUserId(999L, USER_ID)
                .block();

        assertThat(result).isNull();
    }
}