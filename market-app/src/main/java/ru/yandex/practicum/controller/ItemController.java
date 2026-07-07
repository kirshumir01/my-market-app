package ru.yandex.practicum.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.item.ItemRequest;
import ru.yandex.practicum.dto.request.CatalogRequest;
import ru.yandex.practicum.mapper.CatalogRequestMapper;
import ru.yandex.practicum.mapper.ItemRequestMapper;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.service.CartService;
import ru.yandex.practicum.service.ItemService;
import ru.yandex.practicum.utils.SecurityUtils;

import java.security.Principal;

@Validated
@Controller
@AllArgsConstructor
public class ItemController {

    private final CartService cartService;
    private final ItemService itemService;
    private final CatalogRequestMapper catalogMapper;
    private final ItemRequestMapper itemRequestMapper;

    @PostMapping("/items")
    public Mono<String> changeItemCountFromCatalog(ServerWebExchange exchange) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();

        return Mono.zip(
                exchange.getPrincipal().map(Principal::getName),
                exchange.getFormData().defaultIfEmpty(new LinkedMultiValueMap<>())
        ).flatMap(tuple -> {
            String username = tuple.getT1();
            MultiValueMap<String, String> formData = tuple.getT2();

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
                            username,
                            request.getItemId(),
                            request.getAction())
                    .thenReturn(redirectUrl);
        });
    }

    @GetMapping("/items/new")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Rendering> newItemForm(Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);

        return Mono.just(Rendering.view("item-add-form")
                .modelAttribute("isAdmin", isAdmin)
                .build());
    }

    @PostMapping("/items/new")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> createItem(@ModelAttribute ItemRequest request) {
        return itemService.createItem(request)
                .thenReturn("redirect:/items");
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItem(
            @PathVariable("id") Long itemId,
            Authentication authentication
    ) {
        return itemService.getItem(
                SecurityUtils.getUsername(authentication), itemId
                )
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .modelAttribute("isAdmin", SecurityUtils.isAdmin(authentication))
                        .build());
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeItemCountFromItemPage(
            @PathVariable("id") Long id,
            ServerWebExchange exchange
    ) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();

        return Mono.zip(
                exchange.getPrincipal().map(Principal::getName),
                exchange.getFormData().defaultIfEmpty(new LinkedMultiValueMap<>())
        ).flatMap(tuple -> {
            String username = tuple.getT1();
            MultiValueMap<String, String> formData = tuple.getT2();

            CartAction action = itemRequestMapper.getAction(queryParams, formData);

            return cartService.changeItemsCount(username, id, action)
                    .thenReturn("redirect:/items/" + id);
        });
    }
}