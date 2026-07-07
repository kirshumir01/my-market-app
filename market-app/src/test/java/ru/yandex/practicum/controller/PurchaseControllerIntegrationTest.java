package ru.yandex.practicum.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

class PurchaseControllerIntegrationTest extends TestDataConfiguration {

    private static final String USERNAME = "user";
    private static final long USER_ID = 1L;

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

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("POST /buy -> redirect to /orders/{id}?newOrder=true and clear current user's cart")
    void createOrderFromCart_shouldClearCurrentUserCartAfterOrderCreation() {
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

        assertThat(getUserCartItems()).isNotEmpty();

        Long ordersCountBefore = orderRepository.count().block();
        assertThat(ordersCountBefore).isNotNull();

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader()
                .valueMatches("Location", "/orders/\\d+\\?newOrder=true");

        assertThat(getUserCartItems()).isEmpty();

        Long ordersCountAfter = orderRepository.count().block();

        assertThat(ordersCountAfter).isNotNull();
        assertThat(ordersCountAfter).isEqualTo(ordersCountBefore + 1);

        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
    }

    @Test
    @DisplayName("POST /buy -> redirect to /cart/items?paymentError=true when payment fails")
    void createOrderFromCart_whenPaymentFailed_shouldRedirectToCartAndKeepCurrentUserCart() {
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
        int cartSizeBefore = getUserCartItems().size();

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader()
                .valueEquals("Location", "/cart/items?paymentError=true");

        Long ordersCountAfter = orderRepository.count().block();

        assertThat(ordersCountAfter).isEqualTo(ordersCountBefore);
        assertThat(getUserCartItems()).hasSize(cartSizeBefore);

        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
    }

    @Test
    @DisplayName("POST /buy anonymous -> redirect to login")
    void createOrderFromCartWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(paymentClient);
    }

    private List<CartItem> getUserCartItems() {
        return cartItemRepository.findAllByUserId(USER_ID)
                .collectList()
                .block();
    }
}