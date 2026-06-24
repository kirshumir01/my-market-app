package ru.yandex.practicum.config;

import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresContainer {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("test_db")
                .withUsername("test")
                .withPassword("test");

        POSTGRES.start();
    }
}
