package ru.yandex.practicum.cart.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;

@Controller
@AllArgsConstructor
public class CartController {

    private final CartService cartService;


    @GetMapping("/cart/items")
    public String getCart(Model model) {
        CartDto cart = cartService.getCart();

        model.addAttribute("items", cart.getItems());
        model.addAttribute("total", cart.getTotal());

        return "cart";
    }

    @PostMapping("/cart/items")
    public String changeItemCountFromCart(
            @RequestParam(name = "id") long itemId,
            @RequestParam CartAction action,
            Model model) {
        cartService.changeItemsCount(itemId, action);

        CartDto cart = cartService.getCart();

        model.addAttribute("items", cart.getItems());
        model.addAttribute("total", cart.getTotal());

        return "cart";
    }
}
