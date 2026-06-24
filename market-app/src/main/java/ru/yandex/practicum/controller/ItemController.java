package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.item.ItemRequest;
import ru.yandex.practicum.dto.request.CatalogRequest;
import ru.yandex.practicum.mapper.CatalogRequestMapper;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.service.CartService;
import ru.yandex.practicum.service.ItemService;

@Validated
@Controller
@AllArgsConstructor
public class ItemController {

    private final CartService cartService;
    private final ItemService itemService;
    private final CatalogRequestMapper catalogMapper;

    @PostMapping("/items")
    public Mono<String> changeItemCountFromCatalog(ServerWebExchange exchange) {

        MultiValueMap<String, String> queryParams =
                exchange.getRequest().getQueryParams();

        return exchange.getFormData()
                .defaultIfEmpty(new LinkedMultiValueMap<>())
                .flatMap(formData -> {

                    CatalogRequest request = catalogMapper.from(queryParams, formData);

                    String redirectUrl =
                            "redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d"
                                    .formatted(
                                            request.getSearch(),
                                            request.getSort(),
                                            request.getPageNumber(),
                                            request.getPageSize()
                                    );

                    return cartService.changeItemsCount(
                                    request.getItemId(),
                                    request.getAction())
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

    @GetMapping("/items/new")
    public Mono<Rendering> newItemForm() {
        return Mono.just(Rendering.view("item-add-form")
                .modelAttribute("item", new ItemRequest())
                .build());
    }

    @PostMapping("/items/new")
    public Mono<String> createItem(@ModelAttribute ItemRequest request) {
        return itemService.createItem(request)
                .thenReturn("redirect:/items");
    }
}