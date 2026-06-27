package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.service.OrderService;

@Controller
@AllArgsConstructor
public class PurchaseController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public Mono<String> buy() {
        return orderService.createOrderFromCart()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true")
                .onErrorReturn(
                        BadRequestException.class,
                        "redirect:/cart/items?paymentError=true"
                );
    }
}
