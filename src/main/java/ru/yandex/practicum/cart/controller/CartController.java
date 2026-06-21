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
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.exception.BadRequestException;

@Controller
@AllArgsConstructor
public class CartController {

    private final CartService cartService;

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
        MultiValueMap<String, String> params = exchange.getRequest().getQueryParams();

        return exchange.getFormData()
                .defaultIfEmpty(new LinkedMultiValueMap<>())
                .flatMap(formData -> {
                    Long itemId = getLongParam(params, formData, "id");
                    CartAction action = getEnumParam(CartAction.class, params, formData, "action");

                    return cartService.changeItemsCount(itemId, action)
                            .thenReturn("redirect:/cart/items");
                });
    }

    private Long getLongParam(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String... names
    ) {
        for (String name : names) {
            String value = firstValue(queryParams, formData, name);

            if (value != null && !value.isBlank()) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new BadRequestException(
                            "Parameter '%s' must be a number".formatted(name)
                    );
                }
            }
        }

        throw new BadRequestException("Required parameter 'id' is missing");
    }

    private <T extends Enum<T>> T getEnumParam(
            Class<T> enumClass,
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name
    ) {
        String value = firstValue(queryParams, formData, name);

        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    "Required parameter '%s' is missing".formatted(name)
            );
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid value '%s' for parameter '%s'".formatted(value, name)
            );
        }
    }

    private String firstValue(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name
    ) {
        String queryValue = queryParams.getFirst(name);
        if (queryValue != null) {
            return queryValue;
        }

        return formData.getFirst(name);
    }
}
