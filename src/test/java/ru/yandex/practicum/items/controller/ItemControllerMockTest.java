package ru.yandex.practicum.items.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.items.service.ItemService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerMockTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ItemService itemService;

    @MockitoBean
    CartService cartService;

    private static ItemDto item_1;
    private static ItemDto item_2;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
    }

    @Test
    @DisplayName("GET /items/1 -> 200 OK")
    void getItemById_shouldReturnItem() throws Exception {
        when(itemService.getItem(1L)).thenReturn(item_1);

        mvc.perform(get("/items/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", item_1));

        verify(itemService).getItem(1L);
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("GET /items/999 -> 404 NOT FOUND")
    void getNonExistingItemById_shouldNotReturnItem() throws Exception {
        when(itemService.getItem(999L))
                .thenThrow(new NotFoundException("Item with id = 999 not found"));

        mvc.perform(get("/items/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(itemService).getItem(999L);
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /items/2?action=MINUS -> 200 OK")
    void decreaseItemCountFromItemPage_shouldDecreaseCount() throws Exception {
        when(itemService.getItem(2L)).thenReturn(item_2);

        mvc.perform(post("/items/{id}", 2L)
                        .param("action", CartAction.MINUS.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", item_2));

        verify(cartService).changeItemsCount(2L, CartAction.MINUS);
        verify(itemService).getItem(2L);
    }

    @Test
    @DisplayName("POST /items/1?action=PLUS -> 200 OK")
    void increaseItemCountFromItemPage_shouldIncreaseCount() throws Exception {
        when(itemService.getItem(1L)).thenReturn(item_1);

        mvc.perform(post("/items/{id}", 1L)
                        .param("action", CartAction.PLUS.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", item_1));

        verify(cartService).changeItemsCount(1L, CartAction.PLUS);
        verify(itemService).getItem(1L);
    }

    @Test
    @DisplayName("POST /items?search=&sort=NO&pageNumber=1&pageSize=3 -> redirect /items")
    void changeItemCountFromItemCatalogPage_shouldDoRedirect() throws Exception {

        mvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", CartAction.PLUS.name())
                        .param("search", "")
                        .param("sort", ItemSort.NO.name())
                        .param("pageNumber", "1")
                        .param("pageSize", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/items?search=&sort=NO&pageNumber=1&pageSize=3"
                ));

        verify(cartService).changeItemsCount(1L, CartAction.PLUS);
        verifyNoInteractions(itemService);
    }
}