package ru.yandex.practicum.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.config.MarketSecurityConfig;
import ru.yandex.practicum.dto.item.ItemShortDto;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@WebFluxTest(OrderController.class)
@Import({
        MarketSecurityConfig.class,
        ErrorHandler.class
})
class OrderControllerMockTest {

    private static final String USERNAME = "user";

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    ReactiveUserDetailsService reactiveUserDetailsService;

    private static ItemShortDto item_1;
    private static ItemShortDto item_2;
    private static ItemShortDto item_3;
    private static OrderDto order_1;
    private static OrderDto order_2;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemShortDto(1L, "Test item_1 title", 11999L, 4);
        item_2 = new ItemShortDto(2L, "Test item_2 title", 14999L, 3);
        item_3 = new ItemShortDto(3L, "Test item_3 title", 7999L, 2);

        order_1 = new OrderDto();
        order_1.setId(1L);
        order_1.setTotalSum(item_1.getPrice() * item_1.getCount()
                + item_2.getPrice() * item_2.getCount());
        order_1.setItems(List.of(item_1, item_2));

        order_2 = new OrderDto();
        order_2.setId(2L);
        order_2.setTotalSum(item_3.getPrice() * item_3.getCount());
        order_2.setItems(List.of(item_3));
    }

    @Test
    @DisplayName("GET /orders -> 200 OK")
    void getAllOrders_shouldReturnOrders() {
        when(orderService.getOrders(USERNAME))
                .thenReturn(Flux.just(order_1, order_2));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk();

        verify(orderService).getOrders(USERNAME);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders anonymous -> redirect to login")
    void getAllOrdersWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/1 -> 200 OK")
    void getOrderById_shouldReturnOrder() {
        when(orderService.getOrder(USERNAME, 1L))
                .thenReturn(Mono.just(order_1));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri("/orders/{id}", 1L)
                .exchange()
                .expectStatus().isOk();

        verify(orderService).getOrder(USERNAME, 1L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/999 -> 404 NOT FOUND")
    void getOrderByUnexistingId_shouldNotReturnOrder() {
        when(orderService.getOrder(USERNAME, 999L))
                .thenReturn(Mono.error(
                        new NotFoundException("Order with id = 999 not found")
                ));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri("/orders/{id}", 999L)
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).getOrder(USERNAME, 999L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/1?newOrder=true -> 200 OK")
    void getOrderByIdWithNewOrderParam_shouldReturnOrderWithNewOrderFlag() {
        when(orderService.getOrder(USERNAME, 1L))
                .thenReturn(Mono.just(order_1));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/{id}")
                        .queryParam("newOrder", true)
                        .build(1L))
                .exchange()
                .expectStatus().isOk();

        verify(orderService).getOrder(USERNAME, 1L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    @DisplayName("GET /orders/1 anonymous -> redirect to login")
    void getOrderByIdWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .get()
                .uri("/orders/{id}", 1L)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(orderService);
    }
}