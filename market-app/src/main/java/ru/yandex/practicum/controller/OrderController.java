package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.service.OrderService;

import java.security.Principal;

@Controller
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<Rendering> getOrders(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .flatMap(username -> orderService.getOrders(username)
                        .collectList()
                        .map(orders -> Rendering.view("orders")
                                .modelAttribute("orders", orders)
                                .build()));
    }

    @GetMapping("/orders/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Rendering> getOrder(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            ServerWebExchange exchange
    ) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .flatMap(username -> orderService.getOrder(username, id)
                        .map(order -> Rendering.view("order")
                                .modelAttribute("order", order)
                                .modelAttribute("newOrder", newOrder)
                                .build()));
    }
}