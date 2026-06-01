package ru.yandex.practicum.catalog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.dto.ItemsPageDto;
import ru.yandex.practicum.items.dto.PageDto;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.items.repository.ItemRepository;

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

        cartItem_1 = new CartItem(1L, item_1, 2);
        cartItem_2 = new CartItem(2L, item_2, 3);
    }

    @Test
    void getItems_whenSearchIsBlank_shouldReturnPagedItems() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2, item_3),
                PageRequest.of(0, 3),
                4
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(cartItem_1, cartItem_2));

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 3);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0)).hasSize(3);
        assertThat(result.getPaging()).isNotNull();

        verify(itemRepository).findAll(any(Pageable.class));
        verify(itemRepository, never())
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        anyString(),
                        anyString(),
                        any(Pageable.class)
                );
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L, 3L));
    }

    @Test
    void getItems_whenSearchExists_shouldSearchByTitleAndDescription() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2),
                PageRequest.of(0, 3),
                2
        );

        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                eq("item"),
                eq("item"),
                any(Pageable.class)
        )).thenReturn(page);

        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(cartItem_1, cartItem_2));

        ItemsPageDto result = catalogService.getItems("item", ItemSort.NO, 1, 3);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0)).hasSize(3);

        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                eq("item"),
                eq("item"),
                any(Pageable.class)
        );
        verify(itemRepository, never()).findAll(any(Pageable.class));
        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));
    }

    @Test
    void getItems_whenRepositoryReturnsEmptyPage_shouldReturnEmptyItemsList() {
        Page<Item> page = Page.empty(PageRequest.of(0, 3));

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 3);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getPaging()).isNotNull();

        verify(itemRepository).findAll(any(Pageable.class));
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getItems_shouldMapCartItemsCountsCorrectly() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2),
                PageRequest.of(0, 3),
                2
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(cartItem_1, cartItem_2));

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 3);

        List<ItemDto> firstGroup = result.getItems().getFirst();

        assertThat(firstGroup)
                .filteredOn(item -> !item.getId().equals(-1L))
                .extracting(ItemDto::getCount)
                .containsExactlyInAnyOrder(2, 3);

        assertThat(firstGroup)
                .filteredOn(item -> item.getId().equals(-1L))
                .hasSize(1);

        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));
    }

    @Test
    void getItems_whenItemNotExistsInCart_shouldSetCountZero() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2),
                PageRequest.of(0, 3),
                2
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(List.of());

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 3);

        List<ItemDto> firstGroup = result.getItems().getFirst();

        assertThat(firstGroup)
                .filteredOn(item -> !item.getId().equals(-1L))
                .extracting(ItemDto::getCount)
                .containsExactlyInAnyOrder(0, 0);

        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L));
    }

    @Test
    void getItems_shouldCalculatePagingCorrectly() {
        Page<Item> page = new PageImpl<>(
                List.of(item_4),
                PageRequest.of(1, 3),
                4
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(4L))).thenReturn(List.of());

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 2, 3);

        PageDto paging = result.getPaging();

        assertThat(paging).isNotNull();
        assertThat(paging.getPageNumber()).isEqualTo(2);
        assertThat(paging.getPageSize()).isEqualTo(3);
        assertThat(paging.isHasPrevious()).isTrue();
        assertThat(paging.isHasNext()).isFalse();
        verify(itemRepository).findAll(any(Pageable.class));
    }

    @Test
    void getItems_whenSortIsAlpha_shouldSortByTitleAscending() {
        Page<Item> page = new PageImpl<>(List.of(item_1), PageRequest.of(0, 3), 1);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L))).thenReturn(List.of());

        catalogService.getItems("", ItemSort.ALPHA, 1, 3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(3);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();
    }

    @Test
    void getItems_whenSortIsPrice_shouldSortByPriceAscending() {
        Page<Item> page = new PageImpl<>(List.of(item_1), PageRequest.of(0, 3), 1);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L))).thenReturn(List.of());

        catalogService.getItems("", ItemSort.PRICE, 1, 3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getSort().getOrderFor("price")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("price").isAscending()).isTrue();
    }

    @Test
    void getItems_whenSortIsNo_shouldUseUnsortedPageable() {
        Page<Item> page = new PageImpl<>(List.of(item_1), PageRequest.of(0, 3), 1);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L))).thenReturn(List.of());

        catalogService.getItems("", ItemSort.NO, 1, 3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void getItems_shouldGroupItemsByThree() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2, item_3, item_4),
                PageRequest.of(0, 4),
                4
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of());

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 4);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0)).hasSize(3);
        assertThat(result.getItems().get(1)).hasSize(3);
    }

    @Test
    void getItems_whenItemsCountNotMultipleOfThree_shouldAddEmptyDtos() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2, item_3, item_4),
                PageRequest.of(0, 4),
                4
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of());

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 4);

        assertThat(result.getItems()).hasSize(2);

        List<ItemDto> secondGroup = result.getItems().get(1);

        assertThat(secondGroup).hasSize(3);
        assertThat(secondGroup)
                .filteredOn(item -> !item.getId().equals(-1L))
                .hasSize(1);
        assertThat(secondGroup)
                .filteredOn(item -> item.getId().equals(-1L))
                .hasSize(2);
    }

    @Test
    void getItems_whenItemsCountLessThanThree_shouldAddEmptyDtos() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1),
                PageRequest.of(0, 1),
                1
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(List.of(1L))).thenReturn(List.of());

        ItemsPageDto result = catalogService.getItems("", ItemSort.NO, 1, 1);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst()).hasSize(3);
        assertThat(result.getItems().getFirst())
                .filteredOn(item -> !item.getId().equals(-1L))
                .hasSize(1);
    }

    @Test
    void getItems_shouldCallCartRepositoryWithCorrectItemIds() {
        Page<Item> page = new PageImpl<>(
                List.of(item_1, item_2, item_3),
                PageRequest.of(0, 3),
                3
        );

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findAllByItemIdIn(anyList())).thenReturn(List.of());

        catalogService.getItems("", ItemSort.NO, 1, 3);

        verify(cartItemRepository).findAllByItemIdIn(List.of(1L, 2L, 3L));
    }

    @Test
    void getItems_shouldNotCallCartRepositoryWhenItemsPageIsEmpty() {
        Page<Item> page = Page.empty(PageRequest.of(0, 3));

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        catalogService.getItems("", ItemSort.NO, 1, 3);

        verifyNoInteractions(cartItemRepository);
    }
}