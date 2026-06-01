package ru.yandex.practicum.items.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.items.model.ItemSort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/clear.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void changeItemCountFromCatalogTest_shouldChangeItemCountInCart() throws Exception {
        long itemId = 1L;

        CartItem cartItemBefore = cartItemRepository.findByItemIdWithItem(itemId).orElseThrow();

        assertThat(cartItemBefore.getCount()).isEqualTo(2);

        mvc.perform(post("/items")
                .param("id", String.valueOf(itemId))
                .param("action", CartAction.PLUS.name())
                .param("search", "")
                .param("sort", ItemSort.NO.name())
                .param("pageNumber", "1")
                .param("pageSize", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=3"));

        CartItem cartItemAfter = cartItemRepository.findByItemIdWithItem(itemId).orElseThrow();

        assertThat(cartItemAfter.getItem().getId()).isEqualTo(itemId);
        assertThat(cartItemAfter.getCount()).isEqualTo(3);
    }
}