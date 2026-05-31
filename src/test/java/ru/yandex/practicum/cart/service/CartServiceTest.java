package ru.yandex.practicum.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cart.dto.CartDto;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Item item_1;
    private Item item_2;

    private CartItem cartItem_1;
    private CartItem cartItem_2;

    @BeforeEach
    void setUp() {
        item_1 = new Item(1L, "Test item_1 title", "Test item_1 description", null, 999L);
        item_2 = new Item(2L, "Test item_2 title", "Test item_2 description", null, 2999L);

        cartItem_1 = new CartItem(1L, item_1, 2);
        cartItem_2 = new CartItem(2L, item_2, 3);
    }

    @Test
    void getCart_shouldReturnCartWithItemsAndTotal() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of(cartItem_1, cartItem_2));

        CartDto result = cartService.getCart();

        long expectedTotal = item_1.getPrice() * cartItem_1.getCount()
                + item_2.getPrice() * cartItem_2.getCount();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(expectedTotal);

        verify(cartItemRepository).findAllWithItems();
    }

    @Test
    void getCart_whenCartIsEmpty_shouldReturnEmptyCart() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of());

        CartDto result = cartService.getCart();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isZero();

        verify(cartItemRepository).findAllWithItems();
    }

    @Test
    void changeItemsCount_whenActionPlusAndCartItemExists_shouldIncreaseCount() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem_1));

        cartService.changeItemsCount(1L, CartAction.PLUS);

        assertThat(cartItem_1.getCount()).isEqualTo(3);

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
        verify(cartItemRepository).save(cartItem_1);
    }

    @Test
    void changeItemsCount_whenActionPlusAndCartItemDoesNotExist_shouldCreateCartItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        cartService.changeItemsCount(1L, CartAction.PLUS);

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);

        verify(cartItemRepository).save(cartItemCaptor.capture());

        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getItem()).isEqualTo(item_1);
        assertThat(savedCartItem.getCount()).isEqualTo(1);

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
    }

    @Test
    void changeItemsCount_whenActionMinusAndCountGreaterThanOne_shouldDecreaseCount() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem_1));

        cartService.changeItemsCount(1L, CartAction.MINUS);

        assertThat(cartItem_1.getCount()).isEqualTo(1);

        verify(cartItemRepository).save(cartItem_1);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionMinusAndCountEqualsOne_shouldDeleteCartItem() {
        cartItem_1.setCount(1);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem_1));

        cartService.changeItemsCount(1L, CartAction.MINUS);

        verify(cartItemRepository).delete(cartItem_1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionMinusAndCartItemDoesNotExist_shouldDoNothing() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        cartService.changeItemsCount(1L, CartAction.MINUS);

        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionDeleteAndCartItemExists_shouldDeleteCartItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem_1));

        cartService.changeItemsCount(1L, CartAction.DELETE);

        verify(cartItemRepository).delete(cartItem_1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionDeleteAndCartItemDoesNotExist_shouldDoNothing() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        cartService.changeItemsCount(1L, CartAction.DELETE);

        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenItemDoesNotExist_shouldThrowNotFoundException() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> cartService.changeItemsCount(999L, CartAction.PLUS)
        );

        assertThat(exception.getMessage()).isEqualTo("Item with id = 999 not found");

        verify(itemRepository).findById(999L);
        verifyNoInteractions(cartItemRepository);
    }
}