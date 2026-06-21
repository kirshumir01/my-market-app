package ru.yandex.practicum.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

public abstract class TestDataConfiguration extends TestContainersConfiguration {

    @Autowired
    protected DatabaseClient databaseClient;

    @BeforeEach
    void setUpDatabase() {
        clearDatabase();
        insertTestData();
        restartSequences();
    }

    private void clearDatabase() {
        databaseClient.sql("DELETE FROM orders_items").then().block();
        databaseClient.sql("DELETE FROM cart_items").then().block();
        databaseClient.sql("DELETE FROM orders").then().block();
        databaseClient.sql("DELETE FROM items").then().block();
    }

    private void insertTestData() {
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
    }

    private void restartSequences() {
        databaseClient.sql("ALTER TABLE items ALTER COLUMN id RESTART WITH 7").then().block();
        databaseClient.sql("ALTER TABLE orders ALTER COLUMN id RESTART WITH 3").then().block();
        databaseClient.sql("ALTER TABLE orders_items ALTER COLUMN id RESTART WITH 4").then().block();
        databaseClient.sql("ALTER TABLE cart_items ALTER COLUMN id RESTART WITH 3").then().block();
    }
}