package ru.yandex.practicum.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@WebFluxTest(LoginController.class)
class LoginControllerMockTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /login -> returns login page")
    void login_shouldReturnLoginPage() {
        webTestClient.get()
                .uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    assertThat(response.getResponseBody()).isNotNull();
                });
    }

    @Test
    @DisplayName("GET /login?error -> returns login page with error flag")
    void login_shouldReturnLoginPageWithError() {
        webTestClient.get()
                .uri("/login?error")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /login?logout -> returns login page with logout flag")
    void login_shouldReturnLoginPageWithLogout() {
        webTestClient.get()
                .uri("/login?logout")
                .exchange()
                .expectStatus().isOk();
    }
}