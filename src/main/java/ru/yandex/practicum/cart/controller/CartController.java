package ru.yandex.practicum.cart.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.request.dto.CartRequest;
import ru.yandex.practicum.request.mapper.CartRequestMapper;

@Controller
@AllArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartRequestMapper cartRequestMapper;

    @GetMapping("/cart/items")
    public Mono<Rendering> getCart() {
        return cartService.getCart()
                .map(cart -> Rendering.view("cart")
                        .modelAttribute("items", cart.getItems())
                        .modelAttribute("total", cart.getTotal())
                        .build());
    }

    @PostMapping("/cart/items")
    public Mono<String> changeItemCountFromCart(ServerWebExchange exchange) {
        MultiValueMap<String, String> queryParams =
                exchange.getRequest().getQueryParams();

        return exchange.getFormData()
                .defaultIfEmpty(new LinkedMultiValueMap<>())
                .flatMap(formData -> {

                    CartRequest request = cartRequestMapper.from(queryParams, formData);

                    return cartService.changeItemsCount(
                                    request.getItemId(),
                                    request.getAction())
                            .thenReturn("redirect:/cart/items");
                });
    }
}