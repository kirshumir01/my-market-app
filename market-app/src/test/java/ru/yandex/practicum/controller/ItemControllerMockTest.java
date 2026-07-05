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
import reactor.core.publisher.Mono;
import ru.yandex.practicum.config.MarketSecurityConfig;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.CatalogRequestMapper;
import ru.yandex.practicum.mapper.ItemRequestMapper;
import ru.yandex.practicum.mapper.RequestParamMapper;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.service.CartService;
import ru.yandex.practicum.service.ItemService;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@WebFluxTest(ItemController.class)
@Import({
        MarketSecurityConfig.class,
        ErrorHandler.class,
        CatalogRequestMapper.class,
        RequestParamMapper.class
})
class ItemControllerMockTest {

    private static final String USERNAME = "user";

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ItemService itemService;

    @MockitoBean
    CartService cartService;

    @MockitoBean
    ItemRequestMapper itemRequestMapper;

    @MockitoBean
    ReactiveUserDetailsService reactiveUserDetailsService;

    private static ItemDto item_1;
    private static ItemDto item_2;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
    }

    @Test
    @DisplayName("GET /items/1 anonymous -> 200 OK")
    void getItemByIdWithoutAuthentication_shouldReturnItem() {
        when(itemService.getItem(null, 1L))
                .thenReturn(Mono.just(item_1));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus().isOk();

        verify(itemService).getItem(null, 1L);
        verifyNoInteractions(cartService);
        verifyNoMoreInteractions(itemService);
    }

    @Test
    @DisplayName("GET /items/1 authenticated -> 200 OK")
    void getItemByIdWithAuthentication_shouldReturnItem() {
        when(itemService.getItem(USERNAME, 1L))
                .thenReturn(Mono.just(item_1));

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus().isOk();

        verify(itemService).getItem(USERNAME, 1L);
        verifyNoInteractions(cartService);
        verifyNoMoreInteractions(itemService);
    }

    @Test
    @DisplayName("GET /items/999 -> 404 NOT FOUND")
    void getNonExistingItemById_shouldNotReturnItem() {
        when(itemService.getItem(null, 999L))
                .thenReturn(Mono.error(
                        new NotFoundException("Item with id = 999 not found")
                ));

        webTestClient.get()
                .uri("/items/{id}", 999L)
                .exchange()
                .expectStatus().isNotFound();

        verify(itemService).getItem(null, 999L);
        verifyNoInteractions(cartService);
        verifyNoMoreInteractions(itemService);
    }

    @Test
    @DisplayName("POST /items/2?action=MINUS -> 303 SEE_OTHER")
    void decreaseItemCountFromItemPage_shouldDecreaseCount() {
        when(itemRequestMapper.getAction(any(), any()))
                .thenReturn(CartAction.MINUS);

        when(cartService.changeItemsCount(USERNAME, 2L, CartAction.MINUS))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", CartAction.MINUS.name())
                        .build(2L))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/items/2");

        verify(itemRequestMapper).getAction(any(), any());
        verify(cartService).changeItemsCount(USERNAME, 2L, CartAction.MINUS);
        verifyNoInteractions(itemService);
        verifyNoMoreInteractions(cartService, itemRequestMapper);
    }

    @Test
    @DisplayName("POST /items/1?action=PLUS -> 303 SEE_OTHER")
    void increaseItemCountFromItemPage_shouldIncreaseCount() {
        when(itemRequestMapper.getAction(any(), any()))
                .thenReturn(CartAction.PLUS);

        when(cartService.changeItemsCount(USERNAME, 1L, CartAction.PLUS))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", CartAction.PLUS.name())
                        .build(1L))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/items/1");

        verify(itemRequestMapper).getAction(any(), any());
        verify(cartService).changeItemsCount(USERNAME, 1L, CartAction.PLUS);
        verifyNoInteractions(itemService);
        verifyNoMoreInteractions(cartService, itemRequestMapper);
    }

    @Test
    @DisplayName("POST /items/1 anonymous -> redirect to login")
    void changeItemCountFromItemPageWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", CartAction.PLUS.name())
                        .build(1L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(itemService, cartService, itemRequestMapper);
    }

    @Test
    @DisplayName("POST /items?search=&sort=NO&pageNumber=1&pageSize=3 -> redirect /items")
    void changeItemCountFromItemCatalogPage_shouldDoRedirect() {
        when(cartService.changeItemsCount(USERNAME, 1L, CartAction.PLUS))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(USERNAME))
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", 1)
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

        verify(cartService).changeItemsCount(USERNAME, 1L, CartAction.PLUS);
        verifyNoInteractions(itemService);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /items anonymous -> redirect to login")
    void changeItemCountFromCatalogWithoutAuthentication_shouldRedirectToLogin() {
        webTestClient
                .mutateWith(csrf())
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", 1)
                        .queryParam("action", CartAction.PLUS.name())
                        .queryParam("search", "")
                        .queryParam("sort", ItemSort.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");

        verifyNoInteractions(itemService, cartService, itemRequestMapper);
    }
}