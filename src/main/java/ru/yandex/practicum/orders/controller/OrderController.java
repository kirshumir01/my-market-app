package ru.yandex.practicum.orders.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.service.OrderService;

@Controller
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String getOrders(Model model) {
        model.addAttribute("orders", orderService.getOrders());
        return "orders";
    }

    @GetMapping("/orders/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String getOrder(
            @PathVariable(name = "id") long orderId,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model) {
        OrderDto order = orderService.getOrder(orderId);

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }
}