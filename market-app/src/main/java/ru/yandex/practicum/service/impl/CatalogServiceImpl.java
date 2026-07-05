package ru.yandex.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.dto.catalog.CatalogPageRequest;
import ru.yandex.practicum.dto.item.ItemDto;
import ru.yandex.practicum.dto.item.ItemsPageDto;
import ru.yandex.practicum.dto.item.PageDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.ItemMapper;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.ItemSort;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.CatalogService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @Value("${catalog.default-page-size}")
    private int defaultPageSize;

    @Value("${catalog.items-per-row}")
    private int itemsPerRow;

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemsPageDto> getItems(
            String username,
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize
    ) {
        CatalogPageRequest request = buildCatalogPageRequest(search, sort, pageNumber, pageSize);

        return countItems(request)
                .flatMap(totalItems -> findItems(request)
                        .collectList()
                        .flatMap(items -> buildItemsPage(username, items, request, totalItems)));
    }

    private CatalogPageRequest buildCatalogPageRequest(
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize
    ) {
        String safeSearch = search == null ? "" : search.trim();
        ItemSort safeSort = sort == null ? ItemSort.NO : sort;
        int safePageNumber = Math.max(pageNumber, 1);
        int safePageSize = pageSize > 0 ? pageSize : defaultPageSize;

        Pageable pageable = PageRequest.of(
                safePageNumber - 1,
                safePageSize,
                convertSort(safeSort)
        );

        return new CatalogPageRequest(
                safeSearch,
                safeSort,
                safePageNumber,
                safePageSize,
                pageable
        );
    }

    private Mono<Long> countItems(CatalogPageRequest request) {
        if (request.getSearch().isBlank()) {
            return itemRepository.count();
        }

        return itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                request.getSearch(),
                request.getSearch()
        );
    }

    private Flux<Item> findItems(CatalogPageRequest request) {
        return request.getSearch().isBlank()
                ? itemRepository.findAllBy(request.getPageable())
                : itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                        request.getSearch(),
                        request.getSearch(),
                        request.getPageable()
        );
    }

    private Mono<ItemsPageDto> buildItemsPage(
            String username,
            List<Item> pageItems,
            CatalogPageRequest request,
            long totalItems
    ) {
        long offset = (long) (request.getPageNumber() - 1) * request.getPageSize();

        boolean hasPrevious = request.getPageNumber() > 1;
        boolean hasNext = offset + pageItems.size() < totalItems;

        if (pageItems.isEmpty()) {
            return Mono.just(new ItemsPageDto(
                    List.of(),
                    new PageDto(
                            request.getPageSize(),
                            request.getPageNumber(),
                            hasPrevious,
                            false
                    )
            ));
        }

        return getItemsCountInCart(username, pageItems)
                .map(itemsCount -> toItemsPageDto(
                        pageItems,
                        itemsCount,
                        request,
                        hasPrevious,
                        hasNext
                ));
    }

    private Mono<Map<Long, Integer>> getItemsCountInCart(String username, List<Item> pageItems) {
        if (username == null || username.isBlank()) {
            return Mono.just(Map.of());
        }

        List<Long> itemIds = pageItems.stream()
                .map(Item::getId)
                .toList();

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("User with username = %s not found".formatted(username))
                ))
                .flatMapMany(user -> cartItemRepository.findAllByUserIdAndItemIdIn(user.getId(), itemIds))
                .collectList()
                .map(this::toItemsCountMap);
    }

    private Map<Long, Integer> toItemsCountMap(List<CartItem> cartItems) {
        return cartItems.stream()
                .collect(Collectors.toMap(
                        CartItem::getItemId,
                        CartItem::getCount,
                        Integer::sum
                ));
    }

    private Sort convertSort(ItemSort sort) {
        if (sort == ItemSort.ALPHA) {
            return Sort.by("title").ascending();
        }

        if (sort == ItemSort.PRICE) {
            return Sort.by("price").ascending();
        }
        return Sort.unsorted();
    }

    private ItemsPageDto toItemsPageDto(
            List<Item> pageItems,
            Map<Long, Integer> itemsCount,
            CatalogPageRequest request,
            boolean hasPrevious,
            boolean hasNext
    ) {
        List<ItemDto> itemDtoList = pageItems.stream()
                .map(item -> ItemMapper.toItemDto(item, itemsCount))
                .toList();

        return new ItemsPageDto(
                groupingItems(itemDtoList),
                new PageDto(
                        request.getPageSize(),
                        request.getPageNumber(),
                        hasPrevious,
                        hasNext
                )
        );
    }

    private List<List<ItemDto>> groupingItems(List<ItemDto> items) {
        List<List<ItemDto>> groupedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i += itemsPerRow) {
            List<ItemDto> itemsShortList = new ArrayList<>(
                    items.subList(i, Math.min(i + itemsPerRow, items.size()))
            );

            while (itemsShortList.size() < itemsPerRow) {
                itemsShortList.add(toItemEmptyDto());
            }
            groupedItems.add(itemsShortList);
        }
        return groupedItems;
    }

    private ItemDto toItemEmptyDto() {
         return ItemDto.builder()
                 .id(-1L)
                 .title("")
                 .description("")
                 .imgPath("")
                 .price(0L)
                 .count(0)
                 .build();
    }
}