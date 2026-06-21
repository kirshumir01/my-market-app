package ru.yandex.practicum.items.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.items.service.ItemService;

@Validated
@Controller
@AllArgsConstructor
public class ItemController {

    private final CartService cartService;
    private final ItemService itemService;

    @PostMapping("/items")
    public Mono<String> changeItemCountFromCatalog(ServerWebExchange exchange) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();

        return exchange.getFormData()
                .defaultIfEmpty(new LinkedMultiValueMap<>())
                .flatMap(formData -> {
                    Long itemId = getLongParam(queryParams, formData, "id");
                    CartAction action = getEnumParam(CartAction.class, queryParams, formData, "action");
                    String search = getStringParam(queryParams, formData, "search", "");
                    ItemSort sort = getEnumParamOrDefault(ItemSort.class, queryParams, formData, "sort", ItemSort.NO);
                    int pageNumber = getIntParam(queryParams, formData, "pageNumber", 1);
                    int pageSize = getIntParam(queryParams, formData, "pageSize", 5);

                    String redirectUrl = "redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d"
                            .formatted(search, sort, pageNumber, pageSize);

                    return cartService.changeItemsCount(itemId, action)
                            .thenReturn(redirectUrl);
                });
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItem(@PathVariable("id") Long itemId) {
        return itemService.getItem(itemId)
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .modelAttribute("id", item.getId())
                        .build());
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeItemCountFromItemPage(
            @PathVariable("id") Long id,
            @RequestParam CartAction action) {
        return cartService.changeItemsCount(id, action)
                .thenReturn("redirect:/items/" + id);
    }

    private String getStringParam(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name,
            String defaultValue
    ) {
        String value = firstValue(queryParams, formData, name);
        return value == null ? defaultValue : value;
    }

    private int getIntParam(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name,
            int defaultValue
    ) {
        String value = firstValue(queryParams, formData, name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "Parameter '%s' must be an integer".formatted(name)
            );
        }
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

    private <T extends Enum<T>> T getEnumParamOrDefault(
            Class<T> enumClass,
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name,
            T defaultValue
    ) {
        String value = firstValue(queryParams, formData, name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid value '%s' for parameter '%s'".formatted(value, name)
            );
        }
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