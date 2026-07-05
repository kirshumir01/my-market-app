package ru.yandex.practicum.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.config.MarketSecurityConfig;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.service.OrderService;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@WebFluxTest(PurchaseController.class)
@Import({
        MarketSecurityConfig.class,
        ErrorHandler.class
})
class PurchaseControllerMockTest {

    private static final String USERNAME = "user";

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    ReactiveUserDetailsService reactiveUserDetailsService;

    @Test
    @DisplayName("POST /buy -> redirect /orders/{id}?newOrder=true")
    void buyOrder_shouldDoRedirect() {
        when(orderService.createOrderFromCart(USERNAME))
                .thenReturn(Mono.just(1L));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader()
                .location("/orders/1?newOrder=true");

        verify(orderService).createOrderFromCart(USERNAME);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("POST /buy anonymous -> redirect to login")
    void buyOrderWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /buy payment error -> redirect /cart/items?paymentError=true")
    void buyOrder_whenPaymentError_shouldRedirectToCartWithPaymentError() {
        when(orderService.createOrderFromCart(USERNAME))
                .thenReturn(Mono.error(new BadRequestException("Payment failed")));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader()
                .location("/cart/items?paymentError=true");

        verify(orderService).createOrderFromCart(USERNAME);
        verifyNoMoreInteractions(orderService);
    }
}