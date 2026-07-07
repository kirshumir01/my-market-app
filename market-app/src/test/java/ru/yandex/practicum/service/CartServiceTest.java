package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.payment.BalanceResponseDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.model.CartAction;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.impl.CartServiceImpl;
import ru.yandex.practicum.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String USERNAME = TestDataFactory.USERNAME;
    private static final long USER_ID = TestDataFactory.USER_ID;
    private static final long OTHER_USER_ID = TestDataFactory.OTHER_USER_ID;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemCacheService cacheService;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;

    private Item item_1;

    private CartItem cartItem_1;
    private CartItem cartItem_2;

    private ItemCardCacheDto itemCard_1;
    private ItemCardCacheDto itemCard_2;

    private BalanceResponseDto balanceDto;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user();

        item_1 = TestDataFactory.item1();

        cartItem_1 = TestDataFactory.cartItem1();
        cartItem_2 = TestDataFactory.cartItem2();

        itemCard_1 = TestDataFactory.itemCard1();
        itemCard_2 = TestDataFactory.itemCard2();

        balanceDto = TestDataFactory.balanceDto();
    }

    @Test
    @DisplayName("getCartView(username, paymentError) -> returns cart view with items, total and balance")
    void getCartView_shouldReturnCartViewWithItemsTotalAndBalance() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findAllByUserId(USER_ID))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCardCached(1L))
                .thenReturn(Mono.just(itemCard_1));

        when(cacheService.getItemCardCached(2L))
                .thenReturn(Mono.just(itemCard_2));

        when(paymentClient.getBalance(USER_ID))
                .thenReturn(Mono.just(balanceDto));

        long expectedTotal = 999L * 2 + 2999L * 3;

        StepVerifier.create(cartService.getCartView(USERNAME, false))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).hasSize(2);
                    assertThat(result.getTotal()).isEqualTo(expectedTotal);
                    assertThat(result.getBalance()).isEqualTo(balanceDto.getBalance());
                    assertThat(result.getCurrency()).isEqualTo(balanceDto.getCurrency());
                    assertThat(result.isPaymentError()).isFalse();
                    assertThat(result.isPaymentServiceError()).isFalse();

                    assertThat(result.getItems())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L);

                    assertThat(result.getItems())
                            .extracting(ItemDto::getCount)
                            .containsExactly(2, 3);
                })
                .verifyComplete();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findAllByUserId(USER_ID);
        verify(cacheService).getItemCardCached(1L);
        verify(cacheService).getItemCardCached(2L);
        verify(paymentClient).getBalance(USER_ID);
        verifyNoInteractions(itemRepository);
    }

    @Test
    @DisplayName("getCartView(username, paymentError) -> returns empty cart when user cart has no items")
    void getCartView_whenCartIsEmpty_shouldReturnEmptyCart() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findAllByUserId(USER_ID))
                .thenReturn(Flux.empty());

        when(paymentClient.getBalance(USER_ID))
                .thenReturn(Mono.just(balanceDto));

        StepVerifier.create(cartService.getCartView(USERNAME, false))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).isEmpty();
                    assertThat(result.getTotal()).isZero();
                    assertThat(result.getBalance()).isEqualTo(balanceDto.getBalance());
                    assertThat(result.getCurrency()).isEqualTo(balanceDto.getCurrency());
                    assertThat(result.isPaymentError()).isFalse();
                    assertThat(result.isPaymentServiceError()).isFalse();
                })
                .verifyComplete();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findAllByUserId(USER_ID);
        verify(paymentClient).getBalance(USER_ID);
        verifyNoInteractions(cacheService, itemRepository);
    }

    @Test
    @DisplayName("getCartView(username, paymentError) -> throws AccessDeniedException when username is null")
    void getCartView_whenUsernameIsNull_shouldThrowAccessDeniedException() {
        when(userService.getRequiredUser(null))
                .thenReturn(Mono.error(new AccessDeniedException("User is not authenticated")));

        StepVerifier.create(cartService.getCartView(null, false))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(AccessDeniedException.class);
                    assertThat(error.getMessage()).isEqualTo("User is not authenticated");
                })
                .verify();

        verify(userService).getRequiredUser(null);
        verifyNoInteractions(cartItemRepository, cacheService, itemRepository, paymentClient);
    }

    @Test
    @DisplayName("getCartView(username, paymentError) -> throws NotFoundException when user does not exist")
    void getCartView_whenUserDoesNotExist_shouldThrowNotFoundException() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.error(
                        new NotFoundException("User with username = user not found")
                ));

        StepVerifier.create(cartService.getCartView(USERNAME, false))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("User with username = user not found");
                })
                .verify();

        verify(userService).getRequiredUser(USERNAME);
        verifyNoInteractions(cartItemRepository, cacheService, itemRepository, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, PLUS) -> increments item count when cart item exists")
    void changeItemsCount_whenActionPlusAndCartItemExists_shouldIncreaseCount() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.just(cartItem_1));

        when(itemRepository.findById(1L))
                .thenReturn(Mono.just(item_1));

        when(cartItemRepository.save(cartItem_1))
                .thenReturn(Mono.just(cartItem_1));

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.PLUS))
                .verifyComplete();

        assertThat(cartItem_1.getCount()).isEqualTo(3);

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(cartItemRepository).save(cartItem_1);
        verify(itemRepository).findById(1L);
        verifyNoInteractions(cacheService, paymentClient);
        verifyNoMoreInteractions(userService, cartItemRepository, itemRepository);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, PLUS) -> creates cart item when item is not in cart")
    void changeItemsCount_whenActionPlusAndCartItemDoesNotExist_shouldCreateCartItem() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.empty());

        when(itemRepository.findById(1L))
                .thenReturn(Mono.just(item_1));


        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.PLUS))
                .verifyComplete();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);

        verify(cartItemRepository).save(cartItemCaptor.capture());

        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getId()).isNull();
        assertThat(savedCartItem.getUserId()).isEqualTo(USER_ID);
        assertThat(savedCartItem.getItemId()).isEqualTo(1L);
        assertThat(savedCartItem.getCount()).isEqualTo(1);

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(itemRepository).findById(1L);
        verifyNoInteractions(cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, MINUS) -> decrements item count when quantity is greater than one")
    void changeItemsCount_whenActionMinusAndCountGreaterThanOne_shouldDecreaseCount() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.just(cartItem_1));

        when(cartItemRepository.save(cartItem_1))
                .thenReturn(Mono.just(cartItem_1));

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.MINUS))
                .verifyComplete();

        assertThat(cartItem_1.getCount()).isEqualTo(1);

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(cartItemRepository).save(cartItem_1);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verifyNoInteractions(itemRepository, cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, MINUS) -> deletes cart item when quantity equals one")
    void changeItemsCount_whenActionMinusAndCountEqualsOne_shouldDeleteCartItem() {
        cartItem_1.setCount(1);

        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.just(cartItem_1));

        when(cartItemRepository.delete(cartItem_1))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.MINUS))
                .verifyComplete();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(cartItemRepository).delete(cartItem_1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verifyNoInteractions(itemRepository, cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, MINUS) -> throws NotFoundException when cart item does not exist")
    void changeItemsCount_whenActionMinusAndCartItemDoesNotExist_shouldThrowException() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.MINUS))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Cart item with item id = 1 not found");
                })
                .verify();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verifyNoInteractions(itemRepository, cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, DELETE) -> deletes cart item")
    void changeItemsCount_whenActionDeleteAndCartItemExists_shouldDeleteCartItem() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.just(cartItem_1));

        when(cartItemRepository.delete(cartItem_1))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.DELETE))
                .verifyComplete();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(cartItemRepository).delete(cartItem_1);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verifyNoInteractions(itemRepository, cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, DELETE) -> throws NotFoundException when cart item does not exist")
    void changeItemsCount_whenActionDeleteAndCartItemDoesNotExist_shouldThrowException() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 1L, CartAction.DELETE))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Cart item with item id = 1 not found");
                })
                .verify();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verifyNoInteractions(itemRepository, cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount(username, itemId, PLUS) -> throws NotFoundException when item does not exist")
    void changeItemsCount_whenItemDoesNotExist_shouldThrowNotFoundException() {
        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 999L))
                .thenReturn(Mono.empty());

        when(itemRepository.findById(999L))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(USERNAME, 999L, CartAction.PLUS))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Item with id = 999 not found");
                })
                .verify();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 999L);
        verify(itemRepository).findById(999L);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
        verifyNoInteractions(cacheService, paymentClient);
    }

    @Test
    @DisplayName("changeItemsCount() -> user cannot delete another user's cart item")
    void changeItemsCount_whenUserTriesToDeleteOtherUserCartItem_shouldThrowNotFoundException() {
        long itemId = 1L;

        CartItem otherUserCartItem = new CartItem(10L, OTHER_USER_ID, itemId, 1);

        when(userService.getRequiredUser(USERNAME))
                .thenReturn(Mono.just(user));

        when(cartItemRepository.findByUserIdAndItemId(USER_ID, itemId))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeItemsCount(USERNAME, itemId, CartAction.DELETE))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Cart item with item id = 1 not found");
                })
                .verify();

        verify(userService).getRequiredUser(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, itemId);

        verify(cartItemRepository, never())
                .findByUserIdAndItemId(OTHER_USER_ID, itemId);

        verify(cartItemRepository, never())
                .delete(otherUserCartItem);

        verify(cartItemRepository, never())
                .save(any(CartItem.class));

        verifyNoInteractions(itemRepository, cacheService, paymentClient);
    }
}