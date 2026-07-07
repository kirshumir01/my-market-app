package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.request.CartRequest;
import ru.yandex.practicum.mapper.CartRequestMapper;
import ru.yandex.practicum.service.CartService;

import java.security.Principal;

@Controller
@AllArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartRequestMapper cartRequestMapper;

    @GetMapping("/cart/items")
    public Mono<Rendering> getCart(
            @RequestParam(defaultValue = "false") boolean paymentError,
            Principal principal
    ) {
        return cartService.getCartView(principal.getName(), paymentError)
                .map(view -> Rendering.view("cart")
                        .modelAttribute("items", view.getItems())
                        .modelAttribute("total", view.getTotal())
                        .modelAttribute("balance", view.getBalance())
                        .modelAttribute("currency", view.getCurrency())
                        .modelAttribute("canOrder", view.isCanOrder())
                        .modelAttribute("paymentError", view.isPaymentError())
                        .modelAttribute("paymentServiceError", view.isPaymentServiceError())
                        .modelAttribute("authenticated", true)
                        .build());
    }

    @PostMapping("/cart/items")
    public Mono<String> changeItemCountFromCart(
            ServerWebExchange exchange,
            Principal principal
    ) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();

        return exchange.getFormData()
                .defaultIfEmpty(new LinkedMultiValueMap<>())
                .flatMap(formData -> {
                    CartRequest request = cartRequestMapper.from(queryParams, formData);

                    return cartService.changeItemsCount(
                                    principal.getName(),
                                    request.getItemId(),
                                    request.getAction()
                            )
                            .thenReturn("redirect:/cart/items");
                });
    }
}