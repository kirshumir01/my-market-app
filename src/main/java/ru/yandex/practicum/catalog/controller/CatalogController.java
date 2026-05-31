package ru.yandex.practicum.catalog.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.catalog.service.CatalogService;
import ru.yandex.practicum.items.dto.ItemsPageDto;
import ru.yandex.practicum.items.model.ItemSort;

@Validated
@Controller
@AllArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping({"/", "/items"})
    public String getItems(
            @RequestParam(name = "search", defaultValue = "") String search,
            @RequestParam(name = "sort", defaultValue = "NO") ItemSort sort,
            @RequestParam(name = "pageNumber", defaultValue = "1") @Positive int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "5") @Positive int pageSize,
            Model model) {
        ItemsPageDto page = catalogService.getItems(search, sort, pageNumber, pageSize);

        model.addAttribute("items", page.getItems());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", page.getPaging());

        return "items";
    }
}
