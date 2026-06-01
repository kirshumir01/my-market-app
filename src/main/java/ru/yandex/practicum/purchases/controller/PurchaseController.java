package ru.yandex.practicum.purchases.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import ru.yandex.practicum.orders.service.OrderService;

@Controller
@AllArgsConstructor
public class PurchaseController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public String buy() {
        long orderId = orderService.createOrderFromCart();
        return "redirect:/orders/" + orderId + "?newOrder=true";
    }
}
