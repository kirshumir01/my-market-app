package ru.yandex.practicum.items.controller;

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
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.items.service.ItemService;
import ru.yandex.practicum.request.mapper.CatalogRequestMapper;
import ru.yandex.practicum.request.mapper.RequestParamMapper;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@WebFluxTest(ItemController.class)
@Import({
        ErrorHandler.class,
        CatalogRequestMapper.class,
        RequestParamMapper.class
})
class ItemControllerMockTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ItemService itemService;

    @MockitoBean
    CartService cartService;

    private static ItemDto item_1;
    private static ItemDto item_2;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
    }

    @Test
    @DisplayName("GET /items/1 -> 200 OK")
    void getItemById_shouldReturnItem() throws Exception {
        when(itemService.getItem(1L)).thenReturn(Mono.just(item_1));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus().isOk();

        verify(itemService).getItem(1L);
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("GET /items/999 -> 404 NOT FOUND")
    void getNonExistingItemById_shouldNotReturnItem() {
        when(itemService.getItem(999L))
                .thenReturn(Mono.error(
                        new NotFoundException("Item with id = 999 not found")
                ));

        webTestClient.get()
                .uri("/items/{id}", 999L)
                .exchange()
                .expectStatus().isNotFound();

        verify(itemService).getItem(999L);
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /items/2?action=MINUS -> 303 SEE_OTHER")
    void decreaseItemCountFromItemPage_shouldDecreaseCount() {
        when(cartService.changeItemsCount(2L, CartAction.MINUS))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", CartAction.MINUS)
                        .build(2L))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/items/2");

        verify(cartService).changeItemsCount(2L, CartAction.MINUS);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /items/1?action=PLUS -> 200 OK")
    void increaseItemCountFromItemPage_shouldIncreaseCount() {
        when(cartService.changeItemsCount(1L, CartAction.PLUS))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", CartAction.PLUS.name())
                        .build(1L))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/items/1");

        verify(cartService).changeItemsCount(1L, CartAction.PLUS);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /items?search=&sort=NO&pageNumber=1&pageSize=3 -> redirect /items")
    void changeItemCountFromItemCatalogPage_shouldDoRedirect() {
        when(cartService.changeItemsCount(1L, CartAction.PLUS))
                .thenReturn(Mono.empty());

        webTestClient.post()
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
                .expectHeader()
                .location("/items?search=&sort=NO&pageNumber=1&pageSize=3");

        verify(cartService).changeItemsCount(1L, CartAction.PLUS);
        verifyNoInteractions(itemService);
        verifyNoMoreInteractions(cartService);
    }
}