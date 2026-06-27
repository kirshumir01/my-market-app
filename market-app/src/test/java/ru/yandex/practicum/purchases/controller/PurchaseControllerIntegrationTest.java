package ru.yandex.practicum.purchases.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.payment.PaymentRequestDto;
import ru.yandex.practicum.dto.payment.PaymentResponseDto;
import ru.yandex.practicum.dto.payment.PaymentStatus;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseControllerIntegrationTest extends TestDataConfiguration {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ItemCacheService itemCacheService;

    @MockitoBean
    private PaymentClient paymentClient;

    @Test
    @DisplayName("POST /buy -> 3xx Redirect to /orders/{id}?newOrder=true and clear cart")
    void createOrderFromCart_shouldClearCartAfterOrderCreation() {
        when(itemCacheService.getItemCardCached(1L)).thenReturn(Mono.empty());
        when(itemCacheService.getItemCardCached(2L)).thenReturn(Mono.empty());

        when(itemCacheService.saveItemCard(any(ItemCardCacheDto.class)))
                .thenReturn(Mono.just(true));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.PAID,
                        BigDecimal.valueOf(999L * 2 + 2999L * 5),
                        BigDecimal.valueOf(100000),
                        "RUB",
                        "Payment completed successfully",
                        Instant.now()
                )));

        assertThat(getCartItems()).isNotEmpty();

        Long ordersCountBefore = orderRepository.count().block();
        assertThat(ordersCountBefore).isNotNull();

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", "/orders/\\d+\\?newOrder=true");

        assertThat(getCartItems()).isEmpty();

        Long ordersCountAfter = orderRepository.count().block();

        assertThat(ordersCountAfter).isNotNull();
        assertThat(ordersCountAfter).isEqualTo(ordersCountBefore + 1);

        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
    }

    @Test
    @DisplayName("POST /buy -> 3xx Redirect to /cart/items?paymentError=true when payment fails")
    void createOrderFromCart_whenPaymentFailed_shouldRedirectToCartAndKeepCart() {
        when(itemCacheService.getItemCardCached(1L)).thenReturn(Mono.empty());
        when(itemCacheService.getItemCardCached(2L)).thenReturn(Mono.empty());

        when(itemCacheService.saveItemCard(any(ItemCardCacheDto.class)))
                .thenReturn(Mono.just(true));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.FAILED,
                        BigDecimal.valueOf(999L * 2 + 2999L * 5),
                        BigDecimal.valueOf(100),
                        "RUB",
                        "Not enough money on balance",
                        Instant.now()
                )));

        Long ordersCountBefore = orderRepository.count().block();
        int cartSizeBefore = getCartItems().size();

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals("Location", "/cart/items?paymentError=true");

        Long ordersCountAfter = orderRepository.count().block();

        assertThat(ordersCountAfter).isEqualTo(ordersCountBefore);
        assertThat(getCartItems()).hasSize(cartSizeBefore);

        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
    }

    private List<CartItem> getCartItems() {
        return cartItemRepository.findAll()
                .collectList()
                .block();
    }
}