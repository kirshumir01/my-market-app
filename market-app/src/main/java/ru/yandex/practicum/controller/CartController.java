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
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.dto.request.CartRequest;
import ru.yandex.practicum.mapper.CartRequestMapper;
import ru.yandex.practicum.service.CartService;

import java.math.BigDecimal;

@Controller
@AllArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartRequestMapper cartRequestMapper;
    private final PaymentClient paymentClient;

    @GetMapping("/cart/items")
    public Mono<Rendering> getCart(
            @RequestParam(defaultValue = "false") boolean paymentError
    ) {
        return cartService.getCart()
                .flatMap(cart -> paymentClient.getBalance()
                                .map(balance -> {
                                    boolean canOrder = balance.getBalance()
                                            .compareTo(BigDecimal.valueOf(cart.getTotal())) >= 0;

                                    return Rendering.view("cart")
                                            .modelAttribute("items", cart.getItems())
                                            .modelAttribute("total", cart.getTotal())
                                            .modelAttribute("balance", balance.getBalance())
                                            .modelAttribute("currency", balance.getCurrency())
                                            .modelAttribute("canOrder", canOrder)
                                            .modelAttribute("paymentError", paymentError)
                                            .modelAttribute("paymentServiceError", false)
                                            .build();
                                })
                        .onErrorResume(ex -> Mono.just(Rendering.view("cart")
                                .modelAttribute("items", cart.getItems())
                                .modelAttribute("total", cart.getTotal())
                                .modelAttribute("balance", null)
                                .modelAttribute("currency", "RUB")
                                .modelAttribute("canOrder", false)
                                .modelAttribute("paymentError", paymentError)
                                .modelAttribute("paymentServiceError", true)
                                .build())));
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