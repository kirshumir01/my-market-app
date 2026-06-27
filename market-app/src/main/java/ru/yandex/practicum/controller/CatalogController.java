package ru.yandex.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.service.CatalogService;

@Validated
@Controller
@AllArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    private static final int ITEMS_PER_ROW = 3;

    @GetMapping({"/", "/items"})
    public Mono<Rendering> getItems(
            @RequestParam(name = "search", defaultValue = "") String search,
            @RequestParam(name = "sort", defaultValue = "NO") ItemSort sort,
            @RequestParam(name = "pageNumber", defaultValue = "1") @Positive int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "5") @Positive int pageSize,
            Model model) {
        return catalogService.getItems(search, sort, pageNumber, pageSize)
                .map(page -> Rendering.view("items")
                        .modelAttribute("items", page.getItems())
                        .modelAttribute("search", search)
                        .modelAttribute("sort", sort)
                        .modelAttribute("paging", page.getPaging())
                        .modelAttribute("itemsPerRow", ITEMS_PER_ROW)
                        .build());
    }
}
