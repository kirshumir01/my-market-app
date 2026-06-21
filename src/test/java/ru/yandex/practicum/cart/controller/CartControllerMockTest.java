package ru.yandex.practicum.cart.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.items.dto.ItemDto;

import java.util.List;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@WebFluxTest(CartController.class)
@Import(ErrorHandler.class)
class CartControllerMockTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    CartService cartService;

    private static ItemDto item_1;
    private static ItemDto item_2;
    private static ItemDto item_3;
    private static ItemDto item_4;
    private static CartDto fullCart;
    private static CartDto emptyCart;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
        item_3 = new ItemDto(3L, "Test item_3 title", "Test item_3 description", null, 7999L, 2);
        item_4 = new ItemDto(4L, "Test item_4 title", "Test item_4 description", null, 4999L, 4);

        fullCart = new CartDto();

        long totalCartSum = item_1.getPrice() * item_1.getCount()
                + item_2.getPrice() * item_2.getCount()
                + item_3.getPrice() * item_3.getCount()
                + item_4.getPrice() * item_4.getCount();

        fullCart.setItems(List.of(item_1, item_2, item_3, item_4));
        fullCart.setTotal(totalCartSum);

        emptyCart = new CartDto();
        emptyCart.setItems(List.of());
        emptyCart.setTotal(0L);
    }

    @Test
    @DisplayName("GET /cart/items -> 200 OK")
    void getCart_shouldReturnCart() {
        when(cartService.getCart())
                .thenReturn(Mono.just(fullCart));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCart();
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("GET /cart/items -> 200 OK empty cart")
    void getCart_shouldReturnEmptyCart() {
        when(cartService.getCart())
                .thenReturn(Mono.just(emptyCart));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCart();
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=2&action=MINUS -> 200 OK")
    void decreaseItemCountFromCartPage_shouldDecreaseCount() {
        when(cartService.changeItemsCount(2L, CartAction.MINUS))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 2)
                        .queryParam("action", CartAction.MINUS.name())
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).changeItemsCount(2L, CartAction.MINUS);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=1&action=PLUS -> 200 OK")
    void increaseItemCountFromCartPage_shouldIncreaseCount() {
        when(cartService.changeItemsCount(1L, CartAction.PLUS))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 1)
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).changeItemsCount(1L, CartAction.PLUS);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=5&action=DELETE -> 200 OK")
    void deleteItemFromCart_shouldDeleteItemFromCart() {
        when(cartService.changeItemsCount(5L, CartAction.DELETE))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 5)
                        .queryParam("action", CartAction.DELETE.name())
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).changeItemsCount(5L, CartAction.DELETE);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items without id -> 400 BAD REQUEST")
    void changeItemCountFromCartWithoutId_shouldReturnBadRequest() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items without action -> 400 BAD REQUEST")
    void changeItemCountFromCartWithoutAction_shouldReturnBadRequest() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 1)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=1&action=UNKNOWN -> 400 BAD REQUEST")
    void changeItemCountFromCartWithInvalidAction_shouldReturnBadRequest() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 1)
                        .queryParam("action", "UNKNOWN")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService);
    }
}