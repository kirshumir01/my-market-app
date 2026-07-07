package ru.yandex.practicum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class LoginController {

    @GetMapping("/login")
    public Mono<Rendering> login(ServerWebExchange exchange) {
        MultiValueMap<String, String> params =
                exchange.getRequest().getQueryParams();

        return Mono.just(Rendering.view("login")
                .modelAttribute("loginError", params.containsKey("error"))
                .modelAttribute("logoutSuccess", params.containsKey("logout"))
                .build());
    }
}