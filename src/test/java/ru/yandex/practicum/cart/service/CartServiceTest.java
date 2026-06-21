package ru.yandex.practicum.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        cartItem_1 = new CartItem(1L, item_1.getId(), 2);
        cartItem_2 = new CartItem(2L, item_2.getId(), 3);
    }

    @Test
    void getCart_shouldReturnCartWithItemsAndTotal() {
        when(cartItemRepository.findAll())
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(Flux.just(item_1, item_2));

        long expectedTotal = 999L * 2 + 2999L * 3;

        StepVerifier.create(cartService.getCart())
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).hasSize(2);
                    assertThat(result.getTotal()).isEqualTo(expectedTotal);

                    assertThat(result.getItems())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L);

                    assertThat(result.getItems())
                            .extracting(ItemDto::getCount)
                            .containsExactly(2, 3);
                })
                .verifyComplete();

        verify(cartItemRepository).findAll();
        verify(itemRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    void getCart_whenCartIsEmpty_shouldReturnEmptyCart() {
        when(cartItemRepository.findAll())
                .thenReturn(Flux.empty());

        when(itemRepository.findAllById(anyIterable()))
                .thenReturn(Flux.empty());

        StepVerifier.create(cartService.getCart())
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).isEmpty();
                    assertThat(result.getTotal()).isZero();
                })
                .verifyComplete();

        verify(cartItemRepository).findAll();
        verify(itemRepository).findAllById(anyIterable());
        verifyNoMoreInteractions(cartItemRepository, itemRepository);
    }

    @Test
    void changeItemsCount_whenActionPlusAndCartItemExists_shouldIncreaseCount() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem_1));
        when(cartItemRepository.save(cartItem_1)).thenReturn(Mono.just(cartItem_1));

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.PLUS))
                .verifyComplete();

        assertThat(cartItem_1.getCount()).isEqualTo(3);

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
        verify(cartItemRepository).save(cartItem_1);
    }

    @Test
    void changeItemsCount_whenActionPlusAndCartItemDoesNotExist_shouldCreateCartItem() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item_1));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.PLUS))
                .verifyComplete();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);

        verify(cartItemRepository).save(cartItemCaptor.capture());

        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getItemId()).isEqualTo(item_1.getId());
        assertThat(savedCartItem.getCount()).isEqualTo(1);

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
    }

    @Test
    void changeItemsCount_whenActionMinusAndCountGreaterThanOne_shouldDecreaseCount() {
        when(cartItemRepository.findByItemId(1L))
                .thenReturn(Mono.just(cartItem_1));

        when(cartItemRepository.save(cartItem_1))
                .thenReturn(Mono.just(cartItem_1));

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.MINUS))
                .verifyComplete();

        assertThat(cartItem_1.getCount()).isEqualTo(1);

        verify(cartItemRepository).save(cartItem_1);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionMinusAndCountEqualsOne_shouldDeleteCartItem() {
        cartItem_1.setCount(1);

        when(cartItemRepository.findByItemId(1L))
                .thenReturn(Mono.just(cartItem_1));

        when(cartItemRepository.delete(cartItem_1))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.MINUS))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem_1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionMinusAndCartItemDoesNotExist_shouldThrowException() {
        when(cartItemRepository.findByItemId(1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.MINUS))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Cart item with item id = 1 not found");
                })
                .verify();

        verify(cartItemRepository).findByItemId(1L);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionDeleteAndCartItemExists_shouldDeleteCartItem() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem_1));
        when(cartItemRepository.delete(cartItem_1)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.DELETE))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem_1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenActionDeleteAndCartItemDoesNotExist_shouldThrowException() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(1L, CartAction.DELETE))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Cart item with item id = 1 not found");
                })
                .verify();

        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void changeItemsCount_whenItemDoesNotExist_shouldThrowNotFoundException() {
        when(cartItemRepository.findByItemId(999L))
                .thenReturn(Mono.empty());

        when(itemRepository.findById(999L))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(999L, CartAction.PLUS))
                .expectErrorSatisfies(exception -> {
                    assertThat(exception).isInstanceOf(NotFoundException.class);
                    assertThat(exception.getMessage())
                            .isEqualTo("Item with id = 999 not found");
                })
                .verify();

        verify(cartItemRepository).findByItemId(999L);
        verify(itemRepository).findById(999L);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }
}