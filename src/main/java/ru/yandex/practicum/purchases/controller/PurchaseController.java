package ru.yandex.practicum.purchases.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.orders.service.OrderService;

@Controller
@AllArgsConstructor
public class PurchaseController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public Mono<String> buy() {
        return orderService.createOrderFromCart()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
