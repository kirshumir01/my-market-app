package ru.yandex.practicum.items.controller;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.items.service.ItemService;

@Validated
@Controller
@AllArgsConstructor
public class ItemController {

    private final CartService cartService;
    private final ItemService itemService;

    @PostMapping("/items")
    public String changeItemCountFromCatalog(
            @RequestParam(name = "id") long itemId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") ItemSort sort,
            @RequestParam(defaultValue = "1") @Positive int pageNumber,
            @RequestParam(defaultValue = "5") @Positive int pageSize,
            @RequestParam CartAction action,
            RedirectAttributes redirectAttributes) {
        cartService.changeItemsCount(itemId, action);

        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);

        return "redirect:/items";
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable("id") long itemId, Model model) {
        ItemDto item = itemService.getItem(itemId);

        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items/{id}")
    public String changeItemCountFromItemPage(
            @PathVariable(name = "id") long itemId,
            @RequestParam CartAction action,
            Model model) {
        cartService.changeItemsCount(itemId, action);

        ItemDto item = itemService.getItem(itemId);

        model.addAttribute("item", item);
        return "item";
    }
}