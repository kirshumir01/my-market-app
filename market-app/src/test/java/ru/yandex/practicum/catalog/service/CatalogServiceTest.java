package ru.yandex.practicum.catalog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.item.PageDto;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.service.impl.CatalogServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

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
        item_1 = new Item(1L, "Test item_1 title", "Test item_1 description", null, 999L);
        item_2 = new Item(2L, "Test item_2 title", "Test item_2 description", null, 2999L);
        item_3 = new Item(3L, "Test item_3 title", "Test item_3 description", null, 7999L);
        item_4 = new Item(4L, "Test item_4 title", "Test item_4 description", null, 7999L);

        cartItem_1 = new CartItem(1L, item_1.getId(), 2);
        cartItem_2 = new CartItem(2L, item_2.getId(), 3);
    }

    @Test
    void getItems_whenSearchIsBlank_shouldReturnPagedItems() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2, item_3));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L, 3L)))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().get(0)).hasSize(3);
                    assertThat(result.getPaging()).isNotNull();
                    assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
                    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(itemRepository, never())
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                        anyString(),
                        anyString(),
                        any(Pageable.class)
                );
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L, 3L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenSearchExists_shouldSearchByTitleAndDescription() {
        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                eq("item"),
                eq("item"),
                any(Pageable.class)
        )).thenReturn(Flux.just(item_1, item_2));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        StepVerifier.create(catalogService.getItems("item", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().get(0)).hasSize(3);
                })
                .verifyComplete();

        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                eq("item"),
                eq("item"),
                any(Pageable.class)
        );
        verify(itemRepository, never()).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenRepositoryReturnsEmptyPage_shouldReturnEmptyItemsList() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getItems()).isEmpty();
                    assertThat(result.getPaging()).isNotNull();
                    assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
                    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    void getItems_shouldMapCartItemsCountsCorrectly() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    List<ItemDto> firstGroup = result.getItems().get(0);

                    assertThat(firstGroup)
                            .filteredOn(item -> !item.getId().equals(-1L))
                            .extracting(ItemDto::getCount)
                            .containsExactlyInAnyOrder(2, 3);

                    assertThat(firstGroup)
                            .filteredOn(item -> item.getId().equals(-1L))
                            .hasSize(1);
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenItemNotExistsInCart_shouldSetCountZero() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    List<ItemDto> firstGroup = result.getItems().get(0);

                    assertThat(firstGroup)
                            .filteredOn(item -> !item.getId().equals(-1L))
                            .extracting(ItemDto::getCount)
                            .containsExactlyInAnyOrder(0, 0);
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_shouldCalculatePagingCorrectly() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_4));

        when(cartItemRepository.findAllByItemIdIn(List.of(4L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 2, 3))
                .assertNext(result -> {
                    PageDto paging = result.getPaging();

                    assertThat(paging).isNotNull();
                    assertThat(paging.getPageNumber()).isEqualTo(2);
                    assertThat(paging.getPageSize()).isEqualTo(3);
                    assertThat(paging.isHasPrevious()).isTrue();
                    assertThat(paging.isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(4L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenSortIsAlpha_shouldSortByTitleAscending() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.ALPHA, 1, 3))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(itemRepository).findAllBy(pageableCaptor.capture());
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L));

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(4);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();

        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenSortIsPrice_shouldSortByPriceAscending() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.PRICE, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(1);

                    assertThat(result.getItems().getFirst())
                            .hasSize(3);

                    assertThat(result.getItems().getFirst())
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, -1L);
                })
                .verifyComplete();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(itemRepository).findAllBy(pageableCaptor.capture());
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getSort().getOrderFor("price")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("price").isAscending()).isTrue();

        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenSortIsNo_shouldUseUnsortedPageable() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(1);
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

        verify(itemRepository).findAllBy(pageableCaptor.capture());
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L));

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getSort().isUnsorted()).isTrue();

        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_shouldGroupItemsByThree() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2, item_3, item_4));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 4))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(2);
                    assertThat(result.getItems().get(0)).hasSize(3);
                    assertThat(result.getItems().get(1)).hasSize(3);

                    assertThat(result.getItems().get(0))
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, 2L, 3L);

                    assertThat(result.getItems().get(1))
                            .extracting(ItemDto::getId)
                            .containsExactly(4L, -1L, -1L);
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L, 3L, 4L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenItemsCountNotMultipleOfThree_shouldAddEmptyDtos() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2, item_3, item_4));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 4))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(2);

                    List<ItemDto> secondGroup = result.getItems().get(1);

                    assertThat(secondGroup).hasSize(3);

                    assertThat(secondGroup)
                            .filteredOn(item -> !item.getId().equals(-1L))
                            .hasSize(1);

                    assertThat(secondGroup)
                            .filteredOn(item -> item.getId().equals(-1L))
                            .hasSize(2);
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L, 3L, 4L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_whenItemsCountLessThanThree_shouldAddEmptyDtos() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1));

        when(cartItemRepository.findAllByItemIdIn(List.of(1L)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 1))
                .assertNext(result -> {
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().get(0)).hasSize(3);

                    assertThat(result.getItems().get(0))
                            .extracting(ItemDto::getId)
                            .containsExactly(1L, -1L, -1L);
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_shouldCallCartRepositoryWithCorrectItemIds() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(item_1, item_2, item_3));

        when(cartItemRepository.findAllByItemIdIn(anyList()))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L, 3L));
        verifyNoMoreInteractions(itemRepository, cartItemRepository);
    }

    @Test
    void getItems_shouldNotCallCartRepositoryWhenItemsPageIsEmpty() {
        when(itemRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(catalogService.getItems("", ItemSort.NO, 1, 3))
                .assertNext(result -> {
                    assertThat(result.getItems()).isEmpty();
                    assertThat(result.getPaging().isHasNext()).isFalse();
                    assertThat(result.getPaging().isHasPrevious()).isFalse();
                })
                .verifyComplete();

        verify(itemRepository).findAllBy(any(Pageable.class));
        verifyNoInteractions(cartItemRepository);
        verifyNoMoreInteractions(itemRepository);
    }
}