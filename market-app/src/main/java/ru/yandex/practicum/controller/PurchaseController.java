package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.service.OrderService;

import java.security.Principal;

@Controller
@AllArgsConstructor
public class PurchaseController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public Mono<String> buy(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .flatMap(username -> orderService.createOrderFromCart(username)
                        .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true"))
                .onErrorResume(BadRequestException.class,
                        ex -> Mono.just("redirect:/cart/items?paymentError=true"));
    }
}
