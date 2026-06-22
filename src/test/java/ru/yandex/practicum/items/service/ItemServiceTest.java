package ru.yandex.practicum.items.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
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
        cartItem = new CartItem(1L, item.getId(), 3);
    }

    @Test
    void getItem_whenItemExistsAndExistsInCart_shouldReturnItemWithCount() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(itemService.getItem(1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(item.getId());
                    assertThat(result.getTitle()).isEqualTo(item.getTitle());
                    assertThat(result.getDescription()).isEqualTo(item.getDescription());
                    assertThat(result.getPrice()).isEqualTo(item.getPrice());
                    assertThat(result.getCount()).isEqualTo(3);
                })
                .verifyComplete();

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItem_whenItemExistsButNotInCart_shouldReturnItemWithZeroCount() {
        when(itemRepository.findById(1L))
                .thenReturn(Mono.just(item));

        when(cartItemRepository.findByItemId(1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItem(1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(item.getId());
                    assertThat(result.getTitle()).isEqualTo(item.getTitle());
                    assertThat(result.getDescription()).isEqualTo(item.getDescription());
                    assertThat(result.getPrice()).isEqualTo(item.getPrice());
                    assertThat(result.getCount()).isZero();
                })
                .verifyComplete();

        verify(itemRepository).findById(1L);
        verify(cartItemRepository).findByItemId(1L);
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItem_whenItemDoesNotExist_shouldThrowNotFoundException() {
        when(itemRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItem(999L))
                .expectErrorSatisfies(exception -> {
                    assertThat(exception).isInstanceOf(NotFoundException.class);
                    assertThat(exception.getMessage())
                            .isEqualTo("Item with id = 999 not found");
                })
                .verify();

        verify(itemRepository).findById(999L);
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }
}