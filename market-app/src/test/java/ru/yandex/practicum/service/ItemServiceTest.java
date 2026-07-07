package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.impl.ItemServiceImpl;
import ru.yandex.practicum.util.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final String USERNAME = TestDataFactory.USERNAME;
    private static final long USER_ID = TestDataFactory.USER_ID;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemCacheService cacheService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User user;
    private Item item;
    private CartItem cartItem;
    private ItemCardCacheDto itemCard;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user();

        item = TestDataFactory.item1();

        cartItem = TestDataFactory.cartItem1();

        itemCard = TestDataFactory.itemCard1();
    }

    @Test
    @DisplayName("getItem(username, id) -> returns item with cart count when item exists in user's cart")
    void getItem_whenItemExistsInCacheAndExistsInUserCart_shouldReturnItemWithCount() {
        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard));
        when(userService.getRequiredUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(itemService.getItem(USERNAME, 1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(item.getId());
                    assertThat(result.getTitle()).isEqualTo(item.getTitle());
                    assertThat(result.getDescription()).isEqualTo(item.getDescription());
                    assertThat(result.getPrice()).isEqualTo(item.getPrice());
                    assertThat(result.getCount()).isEqualTo(2);
                })
                .verifyComplete();

        verify(cacheService).getItemCardCached(1L);
        verify(userService).getRequiredUserId(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
    }

    @Test
    @DisplayName("getItem(username, id) -> returns item with zero count when item is not in user's cart")
    void getItem_whenItemExistsButNotInUserCart_shouldReturnItemWithZeroCount() {
        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard));
        when(userService.getRequiredUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItem(USERNAME, 1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(item.getId());
                    assertThat(result.getTitle()).isEqualTo(item.getTitle());
                    assertThat(result.getDescription()).isEqualTo(item.getDescription());
                    assertThat(result.getPrice()).isEqualTo(item.getPrice());
                    assertThat(result.getCount()).isZero();
                })
                .verifyComplete();

        verify(cacheService).getItemCardCached(1L);
        verify(userService).getRequiredUserId(USERNAME);
        verify(cartItemRepository).findByUserIdAndItemId(USER_ID, 1L);
    }

    @Test
    @DisplayName("getItem(null, id) -> returns item with zero count for anonymous user")
    void getItem_whenUsernameIsNull_shouldReturnItemWithZeroCount() {
        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard));

        StepVerifier.create(itemService.getItem(null, 1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(item.getId());
                    assertThat(result.getCount()).isZero();
                })
                .verifyComplete();

        verify(cacheService).getItemCardCached(1L);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("getItem(blank username, id) -> returns item with zero count")
    void getItem_whenUsernameIsBlank_shouldReturnItemWithZeroCount() {
        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard));

        StepVerifier.create(itemService.getItem(" ", 1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(item.getId());
                    assertThat(result.getCount()).isZero();
                })
                .verifyComplete();

        verify(cacheService).getItemCardCached(1L);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("getItem(username, id) -> throws NotFoundException when item does not exist")
    void getItem_whenItemDoesNotExist_shouldThrowNotFoundException() {
        when(cacheService.getItemCardCached(999L))
                .thenReturn(Mono.error(new NotFoundException("Item with id = 999 not found")));

        when(userService.getRequiredUserId(USERNAME)).thenReturn(Mono.just(USER_ID));

        StepVerifier.create(itemService.getItem(USERNAME, 999L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("Item with id = 999 not found");
                })
                .verify();

        verify(cacheService).getItemCardCached(999L);
        verify(userService).getRequiredUserId(USERNAME);
    }

    @Test
    @DisplayName("getItem(username, id) -> throws NotFoundException when user does not exist")
    void getItem_whenUserDoesNotExist_shouldThrowNotFoundException() {
        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard));
        when(userService.getRequiredUserId(USERNAME))
                .thenReturn(Mono.error(
                        new NotFoundException("User with username = user not found")
                ));

        StepVerifier.create(itemService.getItem(USERNAME, 1L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("User with username = user not found");
                })
                .verify();

        verify(cacheService).getItemCardCached(1L);
        verify(userService).getRequiredUserId(USERNAME);
        verifyNoInteractions(cartItemRepository);
    }
}