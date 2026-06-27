package ru.yandex.practicum.items.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.repository.CartItemRepository;

import static org.assertj.core.api.Assertions.assertThat;

class ItemControllerIntegrationTest extends TestDataConfiguration {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("POST /items -> increments item count in cart and redirects to catalog")
    void changeItemCountFromCatalogTest_shouldChangeItemCountInCart() {
        long itemId = 1L;

        CartItem cartItemBefore = cartItemRepository.findByItemId(itemId).block();

        assertThat(cartItemBefore).isNotNull();
        assertThat(cartItemBefore.getCount()).isEqualTo(2);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId)
                        .queryParam("action", CartAction.PLUS.name())
                        .queryParam("search", "")
                        .queryParam("sort", ItemSort.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .location("/items?search=&sort=NO&pageNumber=1&pageSize=3");

        CartItem cartItemAfter = cartItemRepository.findByItemId(itemId).block();

        assertThat(cartItemAfter).isNotNull();
        assertThat(cartItemAfter.getItemId()).isEqualTo(itemId);
        assertThat(cartItemAfter.getCount()).isEqualTo(3);
    }
}