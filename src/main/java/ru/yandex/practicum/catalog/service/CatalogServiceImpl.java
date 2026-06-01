package ru.yandex.practicum.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ItemsPageDto getItems(String search, ItemSort sort, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, convertSort(sort));

        Page<Item> page;

        if (search == null || search.isBlank()) {
            page = itemRepository.findAll(pageable);
        } else {
            page = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable);
        }

        List<Item> foundItems = page.get().toList();

        if (foundItems.isEmpty()) {
            PageDto paging = new PageDto(pageSize, pageNumber, page.hasPrevious(), page.hasNext());
            return new ItemsPageDto(List.of(), paging);
        }

        List<ItemDto> itemDtoList = new ArrayList<>();
        List<Long> itemIds = foundItems.stream().map(Item::getId).toList();
        List<CartItem> cartItemList = cartItemRepository.findAllByItemIdIn(itemIds);
        Map<Long, Integer> itemsCount = new HashMap<>();

        cartItemList
                .forEach(cartItem -> {
                    int count = cartItem.getCount();
                    itemsCount.put(
                            cartItem.getItem().getId(),
                            itemsCount.getOrDefault(cartItem.getItem().getId(), 0) + count);
                });

        foundItems
                .forEach(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);
                    itemDto.setCount(itemsCount.getOrDefault(item.getId(), 0));
                    itemDtoList.add(itemDto);
                });

        List<List<ItemDto>> groupedItems = groupingItems(itemDtoList);

        PageDto paging = new PageDto(
                pageSize,
                pageNumber,
                page.hasPrevious(),
                page.hasNext()
        );

        return new ItemsPageDto(groupedItems, paging);
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

        for (int i = 0; i < items.size(); i += 3) {
            List<ItemDto> itemsShortList = new ArrayList<>(
                    items.subList(i, Math.min(i + 3, items.size()))
            );

            while (itemsShortList.size() < 3) {
                itemsShortList.add(ItemMapper.toItemEmptyDto());
            }
            groupedItems.add(itemsShortList);
        }
        return groupedItems;
    }
}
