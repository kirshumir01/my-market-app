package ru.yandex.practicum.purchases.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.controller.PurchaseController;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.service.OrderService;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@WebFluxTest(PurchaseController.class)
@Import(ErrorHandler.class)
class PurchaseControllerMockTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    OrderService orderService;

    @Test
    @DisplayName("POST /buy -> redirect /orders/{id}?newOrder=true")
    void buyOrder_shouldDoRedirect() {
        when(orderService.createOrderFromCart())
                .thenReturn(Mono.just(1L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .location("/orders/1?newOrder=true");

        verify(orderService).createOrderFromCart();
        verifyNoMoreInteractions(orderService);
    }
}