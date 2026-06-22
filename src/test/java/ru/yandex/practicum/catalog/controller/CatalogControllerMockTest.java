package ru.yandex.practicum.catalog.controller;

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
import ru.yandex.practicum.catalog.service.CatalogService;
import ru.yandex.practicum.exception.ErrorHandler;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.dto.ItemsPageDto;
import ru.yandex.practicum.items.dto.PageDto;
import ru.yandex.practicum.items.model.ItemSort;

import java.util.List;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@WebFluxTest(CatalogController.class)
@Import(ErrorHandler.class)
class CatalogControllerMockTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    CatalogService catalogService;

    private static ItemDto item_1;
    private static ItemDto item_2;
    private static ItemDto item_3;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
        item_3 = new ItemDto(3L, "Test item_3 title", "Test item_3 description", null, 7999L, 2);
    }

    @Test
    @DisplayName("GET /items -> 200 OK")
    void getItemsWithoutQueryParameters_shouldReturnAllItems() {
        PageDto paging = new PageDto(5, 1, false, false);

        ItemsPageDto page = new ItemsPageDto(
                List.of(List.of(item_1, item_2, item_3)),
                paging
        );

        when(catalogService.getItems("", ItemSort.NO, 1, 5))
                .thenReturn(Mono.just(page));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();

        verify(catalogService).getItems("", ItemSort.NO, 1, 5);
        verifyNoMoreInteractions(catalogService);
    }

    @Test
    @DisplayName("GET /items?search=test&sort=NO&pageNumber=1&pageSize=3 -> 200 OK")
    void getItemsWithoutSort_shouldReturnItems() throws Exception {
        PageDto paging = new PageDto(3, 1, false, true);
        ItemsPageDto page = new ItemsPageDto(
                List.of(List.of(item_1, item_2, item_3)),
                paging
        );

        when(catalogService.getItems("test", ItemSort.NO, 1, 3)).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test")
                        .queryParam("sort", ItemSort.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(catalogService).getItems("test", ItemSort.NO, 1, 3);
        verifyNoMoreInteractions(catalogService);
    }

    @Test
    @DisplayName("GET /items?search=test&sort=PRICE&pageNumber=1&pageSize=3 -> 200 OK")
    void getItemsWithSortByPrice_shouldReturnItems() throws Exception {
        PageDto paging = new PageDto(3, 1, false, true);

        ItemsPageDto page = new ItemsPageDto(
                List.of(List.of(item_1, item_2, item_3)),
                paging
        );

        when(catalogService.getItems("test", ItemSort.PRICE, 1, 3)).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test")
                        .queryParam("sort", ItemSort.PRICE.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(catalogService).getItems("test", ItemSort.PRICE, 1, 3);
        verifyNoMoreInteractions(catalogService);
    }

    @Test
    @DisplayName("GET /items?search=test&sort=ALPHA&pageNumber=1&pageSize=3 -> 200 OK")
    void getItemsWithSortByTitle_shouldReturnItems() throws Exception {
        PageDto paging = new PageDto(3, 1, false, true);

        ItemsPageDto page = new ItemsPageDto(
                List.of(List.of(item_1, item_2, item_3)),
                paging
        );

        when(catalogService.getItems("test", ItemSort.ALPHA, 1, 3)).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test")
                        .queryParam("sort", ItemSort.ALPHA.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(catalogService).getItems("test", ItemSort.ALPHA, 1, 3);
        verifyNoMoreInteractions(catalogService);
    }

    @Test
    @DisplayName("GET /items?search=test%item%1&sort=NO&pageNumber=1&pageSize=3 -> 200 OK")
    void getItemsBySearchQuery_shouldReturnItems() throws Exception {
        PageDto paging = new PageDto(3, 1, false, false);

        ItemsPageDto page = new ItemsPageDto(
                List.of(List.of(item_1)),
                paging
        );

        when(catalogService.getItems("test item 1", ItemSort.NO, 1, 3)).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "test item 1")
                        .queryParam("sort", ItemSort.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(catalogService).getItems("test item 1", ItemSort.NO, 1, 3);
        verifyNoMoreInteractions(catalogService);
    }

    @Test
    @DisplayName("GET /items?search=item_11&sort=NO&pageNumber=1&pageSize=3 -> 200 OK empty result")
    void getItemsBySearchQuery_shouldNotReturnItems() throws Exception {
        PageDto paging = new PageDto(3, 1, false, false);

        ItemsPageDto page = new ItemsPageDto(List.of(), paging);

        when(catalogService.getItems("item_11", ItemSort.NO, 1, 3)).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "item_11")
                        .queryParam("sort", ItemSort.NO.name())
                        .queryParam("pageNumber", 1)
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(catalogService).getItems("item_11", ItemSort.NO, 1, 3);
        verifyNoMoreInteractions(catalogService);
    }
}