package ru.yandex.practicum.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.items.dto.ItemDto;
import ru.yandex.practicum.items.dto.ItemsPageDto;
import ru.yandex.practicum.items.dto.PageDto;
import ru.yandex.practicum.items.mapper.ItemMapper;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.items.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    private static final int ITEMS_PER_ROW = 3;

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemsPageDto> getItems(String search, ItemSort sort, int pageNumber, int pageSize) {
        String safeSearch = search == null ? "" : search.trim();
        ItemSort safeSort = sort == null ? ItemSort.NO : sort;
        int safePageNumber = Math.max(pageNumber, 1);
        int safePageSize = pageSize > 0 ? pageSize : 5;

        Pageable pageable = PageRequest.of(
                safePageNumber - 1,
                safePageSize + 1,
                convertSort(safeSort)
        );

        Flux<Item> itemsFlux = safeSearch.isBlank()
                ? itemRepository.findAllBy(pageable)
                : itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                safeSearch,
                safeSearch,
                pageable
        );

        return itemsFlux
                .collectList()
                .flatMap(items -> {
                    boolean hasNext = items.size() > safePageSize;
                    boolean hasPrevious = safePageNumber > 1;

                    List<Item> pageItems = items.stream()
                            .limit(safePageSize)
                            .toList();

                    if (pageItems.isEmpty()) {
                        return Mono.just(new ItemsPageDto(
                                List.of(),
                                new PageDto(safePageSize, safePageNumber, hasPrevious, false)
                        ));
                    }

                    List<Long> itemIds = pageItems.stream().map(Item::getId).toList();

                    return cartItemRepository.findAllByItemIdIn(itemIds)
                            .collectList()
                            .map(cartItems -> cartItems.stream()
                                    .collect(Collectors.toMap(
                                            CartItem::getItemId,
                                            CartItem::getCount,
                                            Integer::sum
                                    )))
                            .map(itemsCount -> {
                                List<ItemDto> itemDtoList = pageItems.stream()
                                        .map(item -> {
                                            ItemDto itemDto = ItemMapper.toItemDto(item);
                                            itemDto.setCount(itemsCount.getOrDefault(item.getId(), 0));
                                            return itemDto;
                                        })
                                        .toList();

                                return new ItemsPageDto(
                                        groupingItems(itemDtoList),
                                        new PageDto(
                                                safePageSize,
                                                safePageNumber,
                                                hasPrevious,
                                                hasNext
                                        ));
                            });
                });
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

    private List<List<ItemDto>> groupingItems(List<ItemDto> items) {
        List<List<ItemDto>> groupedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i += ITEMS_PER_ROW) {
            List<ItemDto> itemsShortList = new ArrayList<>(
                    items.subList(i, Math.min(i + ITEMS_PER_ROW, items.size()))
            );

            while (itemsShortList.size() < ITEMS_PER_ROW) {
                itemsShortList.add(ItemMapper.toItemEmptyDto());
            }
            groupedItems.add(itemsShortList);
        }
        return groupedItems;
    }
}
