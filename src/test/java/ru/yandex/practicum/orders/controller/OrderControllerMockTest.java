package ru.yandex.practicum.orders.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemShortDto;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerMockTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService orderService;

    private static ItemShortDto item_1;
    private static ItemShortDto item_2;
    private static ItemShortDto item_3;
    private static OrderDto order_1;
    private static OrderDto order_2;

    @BeforeAll
    static void createItems() {
        item_1 = new ItemShortDto(1L, "Test item_1 title", 11999L, 4);
        item_2 = new ItemShortDto(2L, "Test item_2 title", 14999L, 3);
        item_3 = new ItemShortDto(3L, "Test item_3 title", 7999L, 2);

        order_1 = new OrderDto();
        order_1.setId(1L);
        order_1.setTotalSum(item_1.getPrice() * item_1.getCount() +
                item_2.getPrice() * item_2.getCount());
        order_1.setItems(List.of(item_1, item_2));

        order_2 = new OrderDto();
        order_2.setId(2L);
        order_2.setTotalSum(item_3.getPrice() * item_3.getCount());
        order_2.setItems(List.of(item_3));
    }

    @Test
    @DisplayName("GET /orders -> 200 OK")
    void getAllOrders_shouldReturnOrders() throws Exception {
        List<OrderDto> orders = List.of(order_1, order_2);

        when(orderService.getOrders()).thenReturn(orders);

        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", orders));

        verify(orderService).getOrders();
    }

    @Test
    @DisplayName("GET /orders/1 -> 200 OK")
    void getOrderById_shouldReturnOrder() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(order_1);

        mvc.perform(get("/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("newOrder"))
                .andExpect(model().attribute("order", order_1))
                .andExpect(model().attribute("newOrder", false));

        verify(orderService).getOrder(1L);
    }

    @Test
    @DisplayName("GET /orders/999 -> 404 NOT FOUND")
    void getOrderByUnexistingId_shouldNotReturnOrder() throws Exception {
        when(orderService.getOrder(999L))
                .thenThrow(new NotFoundException("Order with id = 999 not found"));

        mvc.perform(get("/orders/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(orderService).getOrder(999L);
    }

    @Test
    @DisplayName("GET /orders/1?newOrder=true -> 200 OK")
    void getOrderByIdWithNewOrderParam_shouldReturnOrderWithNewOrderFlag() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(order_1);

        mvc.perform(get("/orders/{id}", 1L)
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order_1))
                .andExpect(model().attribute("newOrder", true));

        verify(orderService).getOrder(1L);
    }
}