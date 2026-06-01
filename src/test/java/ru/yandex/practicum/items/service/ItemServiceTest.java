package ru.yandex.practicum.items.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item item;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        item = new Item(1L, "Test item_1 title", "Test item_1 description", null, 999L);
        cartItem = new CartItem(1L, item, 3);
    }

    @Test
    void getItem_whenItemExistsAndExistsInCart_shouldReturnItemWithCount() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        ItemDto result = itemService.getItem(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(item.getId());
        assertThat(result.getTitle()).isEqualTo(item.getTitle());
        assertThat(result.getDescription()).isEqualTo(item.getDescription());
        assertThat(result.getPrice()).isEqualTo(item.getPrice());
        assertThat(result.getCount()).isEqualTo(3);

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItem_whenItemExistsButNotInCart_shouldReturnItemWithZeroCount() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        ItemDto result = itemService.getItem(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(item.getId());
        assertThat(result.getTitle()).isEqualTo(item.getTitle());
        assertThat(result.getDescription()).isEqualTo(item.getDescription());
        assertThat(result.getPrice()).isEqualTo(item.getPrice());
        assertThat(result.getCount()).isZero();

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItem_whenItemDoesNotExist_shouldThrowNotFoundException() {
        when(itemRepository.findById(999L))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> itemService.getItem(999L)
        );

        assertThat(exception.getMessage()).isEqualTo("Item with id = 999 not found");

        verify(itemRepository).findById(999L);
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }
}