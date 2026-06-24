package ru.yandex.practicum.items.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.ItemSort;

import static org.assertj.core.api.Assertions.assertThat;

class ItemControllerIntegrationTest extends TestDataConfiguration {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    DatabaseClient databaseClient;

    @BeforeEach
    void setUp() {
        databaseClient.sql("DELETE FROM orders_items").then().block();
        databaseClient.sql("DELETE FROM cart_items").then().block();
        databaseClient.sql("DELETE FROM orders").then().block();
        databaseClient.sql("DELETE FROM items").then().block();

        databaseClient.sql("""
            INSERT INTO items (id, title, description, img_path, price) VALUES
            (1, 'Test item_1 title', 'Test item_1 description', NULL, 999),
            (2, 'Test item_2 title', 'Test item_2 description', NULL, 2999),
            (3, 'Test item_3 title', 'Test item_3 description', NULL, 7999),
            (4, 'Test item_4 title', 'Test item_4 description', NULL, 4999),
            (5, 'Test item_5 title', 'Test item_5 description', NULL, 11999),
            (6, 'Item_6 title', 'Item_6 description', NULL, 14999)
            """).then().block();

        databaseClient.sql("""
            INSERT INTO orders (id, total_sum) VALUES
            (1, 4997),
            (2, 23997)
            """).then().block();

        databaseClient.sql("""
            INSERT INTO orders_items (id, order_id, item_id, count, price) VALUES
            (1, 1, 1, 2, 999),
            (2, 1, 2, 1, 2999),
            (3, 2, 3, 3, 7999)
            """).then().block();

        databaseClient.sql("""
            INSERT INTO cart_items(id, item_id, count) VALUES
            (1, 1, 2),
            (2, 2, 5)
            """).then().block();

        databaseClient.sql("ALTER TABLE items ALTER COLUMN id RESTART WITH 7").then().block();
        databaseClient.sql("ALTER TABLE orders ALTER COLUMN id RESTART WITH 3").then().block();
        databaseClient.sql("ALTER TABLE orders_items ALTER COLUMN id RESTART WITH 4").then().block();
        databaseClient.sql("ALTER TABLE cart_items ALTER COLUMN id RESTART WITH 3").then().block();
    }

    @Test
    void changeItemCountFromCatalogTest_shouldChangeItemCountInCart() {
        long itemId = 1L;

        CartItem cartItemBefore = cartItemRepository.findByItemId(itemId).block();

        assertThat(cartItemBefore).isNotNull();
        assertThat(cartItemBefore.getCount()).isEqualTo(2);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId)
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

        CartItem cartItemAfter = cartItemRepository.findByItemId(itemId).block();

        assertThat(cartItemAfter).isNotNull();
        assertThat(cartItemAfter.getItemId()).isEqualTo(itemId);
        assertThat(cartItemAfter.getCount()).isEqualTo(3);
    }
}