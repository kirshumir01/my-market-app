package ru.yandex.practicum.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.config.MarketSecurityConfig;
import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.cart.CartViewDto;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.mapper.CartMapper;
import ru.yandex.practicum.mapper.CartRequestMapper;
import ru.yandex.practicum.mapper.RequestParamMapper;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.service.CartService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@WebFluxTest(CartController.class)
@Import({
        MarketSecurityConfig.class,
        ErrorHandler.class,
        CartRequestMapper.class,
        RequestParamMapper.class
})
class CartControllerMockTest {

    private static final String USERNAME = "user";
    private static final String CURRENCY = "RUB";
    private static final BigDecimal BALANCE = BigDecimal.valueOf(10000);

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    CartService cartService;

    @MockitoBean
    ReactiveUserDetailsService reactiveUserDetailsService;

    private static ItemDto item_1;
    private static ItemDto item_2;
    private static ItemDto item_3;
    private static ItemDto item_4;
    private static CartDto fullCart;
    private static CartDto emptyCart;
    private static CartViewDto fullCartView;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
        item_3 = new ItemDto(3L, "Test item_3 title", "Test item_3 description", null, 7999L, 2);
        item_4 = new ItemDto(4L, "Test item_4 title", "Test item_4 description", null, 4999L, 4);

        long totalCartSum = item_1.getPrice() * item_1.getCount()
                + item_2.getPrice() * item_2.getCount()
                + item_3.getPrice() * item_3.getCount()
                + item_4.getPrice() * item_4.getCount();

        fullCart = new CartDto();
        fullCart.setItems(List.of(item_1, item_2, item_3, item_4));
        fullCart.setTotal(totalCartSum);

        emptyCart = new CartDto();
        emptyCart.setItems(List.of());
        emptyCart.setTotal(0L);

        fullCartView = CartMapper.toCartViewDto(
                fullCart,
                BALANCE,
                CURRENCY,
                false,
                false
        );
    }

    @Test
    @DisplayName("GET /cart/items anonymous -> redirect to login")
    void getCartWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("GET /cart/items -> 200 OK")
    void getCart_shouldReturnCart() {
        when(cartService.getCartView(USERNAME, false))
                .thenReturn(Mono.just(fullCartView));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(cartService).getCartView(USERNAME, false);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("GET /cart/items?paymentError=true -> 200 OK")
    void getCartWithPaymentError_shouldReturnCart() {
        CartViewDto cartViewWithPaymentError = CartMapper.toCartViewDto(
                fullCart,
                BALANCE,
                CURRENCY,
                true,
                false
        );

        when(cartService.getCartView(USERNAME, true))
                .thenReturn(Mono.just(cartViewWithPaymentError));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri("/cart/items?paymentError=true")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCartView(USERNAME, true);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=2&action=MINUS -> 303 SEE OTHER")
    void decreaseItemCountFromCartPage_shouldDecreaseCount() {
        when(cartService.changeItemsCount(USERNAME, 2L, CartAction.MINUS))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 2)
                        .queryParam("action", CartAction.MINUS.name())
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).changeItemsCount(USERNAME, 2L, CartAction.MINUS);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=1&action=PLUS -> 303 SEE OTHER")
    void increaseItemCountFromCartPage_shouldIncreaseCount() {
        when(cartService.changeItemsCount(USERNAME, 1L, CartAction.PLUS))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 1)
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).changeItemsCount(USERNAME, 1L, CartAction.PLUS);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=5&action=DELETE -> 303 SEE OTHER")
    void deleteItemFromCart_shouldDeleteItemFromCart() {
        when(cartService.changeItemsCount(USERNAME, 5L, CartAction.DELETE))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 5)
                        .queryParam("action", CartAction.DELETE.name())
                        .build())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).changeItemsCount(USERNAME, 5L, CartAction.DELETE);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items anonymous -> redirect to login")
    void changeItemCountFromCartWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 1)
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items without id -> 400 BAD REQUEST")
    void changeItemCountFromCartWithoutId_shouldReturnBadRequest() {
        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
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
        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", 1)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService);
    }
}