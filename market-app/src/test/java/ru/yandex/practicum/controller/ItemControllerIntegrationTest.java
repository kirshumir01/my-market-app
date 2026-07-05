package ru.yandex.practicum.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.repository.CartItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

class ItemControllerIntegrationTest extends TestDataConfiguration {

    private static final String USERNAME = "user";
    private static final long USER_ID = 1L;
    private static final long ITEM_ID = 1L;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("POST /items -> increments current user's item count in cart and redirects to catalog")
    void changeItemCountFromCatalogTest_shouldChangeItemCountInCart() {
        CartItem cartItemBefore = cartItemRepository
                .findByUserIdAndItemId(USER_ID, ITEM_ID)
                .block();

        assertThat(cartItemBefore).isNotNull();
        assertThat(cartItemBefore.getUserId()).isEqualTo(USER_ID);
        assertThat(cartItemBefore.getItemId()).isEqualTo(ITEM_ID);
        assertThat(cartItemBefore.getCount()).isEqualTo(2);

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", ITEM_ID)
                        .queryParam("action", CartAction.PLUS.name())
                        .queryParam("search", "")
                        .queryParam("sort", ItemSort.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader()
                .location("/items?search=&sort=NO&pageNumber=1&pageSize=3");

        CartItem cartItemAfter = cartItemRepository
                .findByUserIdAndItemId(USER_ID, ITEM_ID)
                .block();

        assertThat(cartItemAfter).isNotNull();
        assertThat(cartItemAfter.getUserId()).isEqualTo(USER_ID);
        assertThat(cartItemAfter.getItemId()).isEqualTo(ITEM_ID);
        assertThat(cartItemAfter.getCount()).isEqualTo(3);
    }
}