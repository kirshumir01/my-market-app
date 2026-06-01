package ru.yandex.practicum.cart.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.service.CartService;
import ru.yandex.practicum.items.dto.ItemDto;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerMockTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartService cartService;

    private static ItemDto item_1;
    private static ItemDto item_2;
    private static ItemDto item_3;
    private static ItemDto item_4;
    private static CartDto fullCart;
    private static CartDto emptyCart;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemDto(1L, "Test item_1 title", "Test item_1 description", null, 999L, 1);
        item_2 = new ItemDto(2L, "Test item_2 title", "Test item_2 description", null, 2999L, 3);
        item_3 = new ItemDto(3L, "Test item_3 title", "Test item_3 description", null, 7999L, 2);
        item_4 = new ItemDto(4L, "Test item_4 title", "Test item_4 description", null, 4999L, 4);

        fullCart = new CartDto();
        long totalCartSum = item_1.getPrice() * item_1.getCount() +
                item_2.getPrice() * item_2.getCount() +
                item_3.getPrice() * item_3.getCount() +
                item_4.getPrice() * item_4.getCount();
        fullCart.setItems(List.of(item_1, item_2, item_3, item_4));
        fullCart.setTotal(totalCartSum);

        emptyCart = new CartDto();
        emptyCart.setItems(List.of());
        emptyCart.setTotal(0L);
    }

    @Test
    @DisplayName("GET /cart/items -> 200 OK")
    void getCart_shouldReturnCart() throws Exception {
        when(cartService.getCart()).thenReturn(fullCart);

        mvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", fullCart.getItems()))
                .andExpect(model().attribute("total", fullCart.getTotal()));

        verify(cartService).getCart();
    }

    @Test
    @DisplayName("GET /cart/items -> 200 OK empty cart")
    void getCart_shouldReturnEmptyCart() throws Exception {
        when(cartService.getCart()).thenReturn(emptyCart);

        mvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", List.of()))
                .andExpect(model().attribute("total", 0L));

        verify(cartService).getCart();
    }

    @Test
    @DisplayName("POST /cart/items?id=2&action=MINUS -> 200 OK")
    void decreaseItemCountFromCartPage_shouldDecreaseCount() throws Exception {
        when(cartService.getCart()).thenReturn(fullCart);

        mvc.perform(post("/cart/items")
                        .param("id", "2")
                        .param("action", CartAction.MINUS.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", fullCart.getItems()))
                .andExpect(model().attribute("total", fullCart.getTotal()));

        verify(cartService).changeItemsCount(2L, CartAction.MINUS);
        verify(cartService).getCart();
    }

    @Test
    @DisplayName("POST /cart/items?id=1&action=PLUS -> 200 OK")
    void increaseItemCountFromCartPage_shouldIncreaseCount() throws Exception {
        when(cartService.getCart()).thenReturn(fullCart);

        mvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", CartAction.PLUS.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", fullCart.getItems()))
                .andExpect(model().attribute("total", fullCart.getTotal()));

        verify(cartService).changeItemsCount(1L, CartAction.PLUS);
        verify(cartService).getCart();
    }

    @Test
    @DisplayName("POST /cart/items?id=5&action=DELETE -> 200 OK")
    void deleteItemFromCart_shouldDeleteItemFromCart() throws Exception {
        when(cartService.getCart()).thenReturn(fullCart);

        mvc.perform(post("/cart/items")
                        .param("id", "5")
                        .param("action", CartAction.DELETE.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", fullCart.getItems()))
                .andExpect(model().attribute("total", fullCart.getTotal()));

        verify(cartService).changeItemsCount(5L, CartAction.DELETE);
        verify(cartService).getCart();
    }

    @Test
    @DisplayName("POST /cart/items without id -> 400 BAD REQUEST")
    void changeItemCountFromCartWithoutId_shouldReturnBadRequest() throws Exception {
        mvc.perform(post("/cart/items")
                        .param("action", CartAction.PLUS.name()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items without action -> 400 BAD REQUEST")
    void changeItemCountFromCartWithoutAction_shouldReturnBadRequest() throws Exception {
        mvc.perform(post("/cart/items")
                        .param("id", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /cart/items?id=1&action=UNKNOWN -> 400 BAD REQUEST")
    void changeItemCountFromCartWithInvalidAction_shouldReturnBadRequest() throws Exception {
        mvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }
}