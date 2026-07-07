package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.item.PageDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.impl.CatalogServiceImpl;
import ru.yandex.practicum.util.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    private static final String USERNAME = TestDataFactory.USERNAME;
    private static final long USER_ID = TestDataFactory.USER_ID;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private Item item_1;
    private Item item_2;
    private Item item_3;
    private Item item_4;

    private CartItem cartItem_1;
    private CartItem cartItem_2;

    @BeforeEach
    void setUp() {
        item_1 = TestDataFactory.item1();
        item_2 = TestDataFactory.item2();
        item_3 = TestDataFactory.item3();
        item_4 = TestDataFactory.item4();

        cartItem_1 = TestDataFactory.cartItem1();
        cartItem_2 = TestDataFactory.cartItem2();

        ReflectionTestUtils.setField(catalogService, "defaultPageSize", 5);
        ReflectionTestUtils.setField(catalogService, "itemsPerRow", 3);
    }

    @Test
    @DisplayName("getItems(null) -> returns items with zero cart counts")
    void getItems_whenAnonymousUser_shouldNotRequestCartItems() {
        when(itemRepository.count())
                .thenReturn(Mono.just(2L));

        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2));

        StepVerifier.create(catalogService.getItems(null, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    List<ItemDto> firstGroup = result.getItems().getFirst();

                    assertThat(firstGroup)
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, -1L);

                    assertThat(firstGroup)
                            .extracting(ItemDto::getCount)
                            .containsExactly(0, 0, 0);
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> returns paged items when search query is blank")
    void getItems_whenSearchIsBlank_shouldReturnPagedItems() {
        when(itemRepository.count()).thenReturn(Mono.just(3L));

        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1, item_2, item_3));

        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));

        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L, 3L)))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().getFirst()).hasSize(3);
                    assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
                    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L, 3L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> returns items matching search query")
    void getItems_whenSearchExists_shouldSearchByTitleAndDescription() {
        when(itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("item", "item"))
                .thenReturn(Mono.just(2L));

        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                eq("item"),
                eq("item"),
                any(Pageable.class)
        )).thenReturn(Flux.just(item_1, item_2));

        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));

        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L)))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        StepVerifier.create(catalogService.getItems(USERNAME, "item", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(1);

                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, -1L);

                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getCount)
                            .containsExactly(2, 3, 0);
                })
                .verifyComplete();

        verify(itemRepository).countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("item", "item");
        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                eq("item"),
                eq("item"),
                any(Pageable.class)
        );
        verify(itemRepository, never()).count();
        verify(itemRepository, never()).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> returns empty items list when repository is empty")
    void getItems_whenRepositoryReturnsEmptyPage_shouldReturnEmptyItemsList() {
        when(itemRepository.count()).thenReturn(Mono.just(0L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems()).isEmpty();
                    assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
                    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verifyNoInteractions(userService, cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> maps cart item counts correctly")
    void getItems_shouldMapCartItemsCountsCorrectly() {
        when(itemRepository.count()).thenReturn(Mono.just(2L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1, item_2));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L)))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    List<ItemDto> firstGroup = result.getItems().getFirst();

                    assertThat(firstGroup)
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, -1L);

                    assertThat(firstGroup)
                            .extracting(ItemDto::getCount)
                            .containsExactly(2, 3, 0);
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> sets zero count for items not present in cart")
    void getItems_whenItemNotExistsInCart_shouldSetCountZero() {
        when(itemRepository.count()).thenReturn(Mono.just(2L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1, item_2));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    List<ItemDto> firstGroup = result.getItems().getFirst();

                    assertThat(firstGroup)
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, -1L);

                    assertThat(firstGroup)
                            .extracting(ItemDto::getCount)
                            .containsExactly(0, 0, 0);
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> calculates paging information correctly")
    void getItems_shouldCalculatePagingCorrectly() {
        when(itemRepository.count()).thenReturn(Mono.just(1L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_4));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(4L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 2, 3))
                .assertNext(result -> {
                    PageDto paging = result.getPaging();

                    assertThat(paging.getPageNumber()).isEqualTo(2);
                    assertThat(paging.getPageSize()).isEqualTo(3);
                    assertThat(paging.isHasPrevious()).isTrue();
                    assertThat(paging.isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(4L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> applies title ascending sort")
    void getItems_whenSortIsAlpha_shouldSortByTitleAscending() {
        when(itemRepository.count()).thenReturn(Mono.just(1L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.ALPHA, 1, 3))
                .assertNext(result -> assertThat(result.getItems().getFirst())
                        .extracting(ItemDto::getId)
                        .containsExactly(1L, -1L, -1L))
                .verifyComplete();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(pageableCaptor.capture());
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L));

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(3);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();

        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> applies price ascending sort")
    void getItems_whenSortIsPrice_shouldSortByPriceAscending() {
        when(itemRepository.count()).thenReturn(Mono.just(2L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1, item_2));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.PRICE, 1, 3))
                .assertNext(result -> assertThat(result.getItems().getFirst())
                        .extracting(ItemDto::getId)
                        .containsExactly(1L, 2L, -1L))
                .verifyComplete();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(pageableCaptor.capture());
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L));

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(3);
        assertThat(pageable.getSort().getOrderFor("price")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("price").isAscending()).isTrue();

        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> returns items without sorting")
    void getItems_whenSortIsNo_shouldUseUnsortedPageable() {
        when(itemRepository.count()).thenReturn(Mono.just(1L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, -1L, -1L);

                    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
                    assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                })
                .verifyComplete();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(pageableCaptor.capture());
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L));

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(3);
        assertThat(pageable.getSort().isUnsorted()).isTrue();

        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> groups items by rows")
    void getItems_shouldGroupItemsByRows() {
        when(itemRepository.count()).thenReturn(Mono.just(4L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1, item_2, item_3, item_4));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 4))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(2);

                    assertThat(result.getItems().get(0))
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, 3L);

                    assertThat(result.getItems().get(1))
                            .extracting(ItemDto::getId)
                            .containsExactly(4L, -1L, -1L);
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L, 3L, 4L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> returns single padded group when items count is less than row size")
    void getItems_whenItemsCountLessThanThree_shouldReturnPaddedGroup() {
        when(itemRepository.count()).thenReturn(Mono.just(1L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1));
        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 1))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().getFirst()).hasSize(3);

                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, -1L, -1L);
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(USER_ID, List.of(1L));
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> requests cart items using current user id and page item ids")
    void getItems_shouldCallCartRepositoryWithCorrectUserIdAndItemIds() {
        when(itemRepository.count()).thenReturn(Mono.just(3L));

        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1, item_2, item_3));

        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.just(USER_ID));

        when(cartItemRepository.findAllByUserIdAndItemIdIn(eq(USER_ID), anyList())).thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verify(cartItemRepository).findAllByUserIdAndItemIdIn(
                USER_ID,
                List.of(1L, 2L, 3L)
        );
        verifyNoMoreInteractions(itemRepository, userService, cartItemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> does not request cart items when page is empty")
    void getItems_shouldNotCallCartRepositoryWhenItemsPageIsEmpty() {
        when(itemRepository.count()).thenReturn(Mono.just(0L));
        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems()).isEmpty();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verifyNoInteractions(userService, cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("getItems(null) -> does not request user and cart items")
    void getItems_whenUsernameIsNull_shouldNotRequestUserAndCartItems() {
        when(itemRepository.count()).thenReturn(Mono.just(2L));
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2));

        StepVerifier.create(catalogService.getItems(null, "", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, -1L);

                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getCount)
                            .containsExactly(0, 0, 0);
                })
                .verifyComplete();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verifyNoInteractions(userService, cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("getItems(username) -> throws NotFoundException when user does not exist")
    void getItems_whenUserDoesNotExist_shouldThrowNotFoundException() {
        when(itemRepository.count()).thenReturn(Mono.just(1L));

        when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.just(item_1));

        when(userService.getOptionalUserId(USERNAME)).thenReturn(Mono.error(
                        new NotFoundException("User with username = user not found")
                ));

        StepVerifier.create(catalogService.getItems(USERNAME, "", ItemSort.NO, 1, 3))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage())
                            .isEqualTo("User with username = user not found");
                })
                .verify();

        verify(itemRepository).count();
        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(userService).getOptionalUserId(USERNAME);
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(itemRepository, userService);
    }
}