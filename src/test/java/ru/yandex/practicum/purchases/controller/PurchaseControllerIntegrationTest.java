package ru.yandex.practicum.purchases.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.orders.repository.OrderRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseControllerIntegrationTest extends TestDataConfiguration {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrderFromCart_shouldClearCartAfterOrderCreation() throws Exception {
        assertThat(getCartItems()).isNotEmpty();

        Long ordersCountBefore = orderRepository.count().block();

        assertThat(ordersCountBefore).isNotNull();

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches(
                        "Location",
                        "/orders/\\d+\\?newOrder=true"
                );

        assertThat(getCartItems()).isEmpty();

        Long ordersCountAfter = orderRepository.count().block();

        assertThat(ordersCountAfter).isNotNull();
        assertThat(ordersCountAfter).isEqualTo(ordersCountBefore + 1);
    }

    private List<CartItem> getCartItems() {
        return cartItemRepository.findAll()
                .collectList()
                .block();
    }
}