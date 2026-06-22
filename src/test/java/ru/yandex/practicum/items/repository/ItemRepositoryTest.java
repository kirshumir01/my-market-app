package ru.yandex.practicum.items.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.items.model.Item;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends TestDataConfiguration {

    @Autowired
    ItemRepository itemRepository;

    @Test
    void findByTitleOrDescriptionIsCaseInsensitive_shouldReturnPageableSort() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("price").ascending());

        List<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                        "test",
                        "test",
                        pageable)
                .collectList()
                .block();

        Long totalElements = itemRepository
                .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "test",
                        "test")
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        assertThat(totalElements).isNotNull();
        assertThat(totalElements).isEqualTo(5);

        assertThat(result)
                .extracting(Item::getTitle)
                .containsExactly(
                        "Test item_1 title",
                        "Test item_2 title",
                        "Test item_4 title",
                        "Test item_3 title",
                        "Test item_5 title"
                );

        assertThat(result)
                .extracting(Item::getPrice)
                .containsExactly(999L, 2999L, 4999L, 7999L, 11999L);
    }

    @Test
    void findByTitleOrDescription_shouldReturnSeveralItems() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                        "test",
                        "test",
                        pageable)
                .collectList()
                .block();

        Long totalElements = itemRepository
                .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "test",
                        "test")
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        assertThat(totalElements).isNotNull();
        assertThat(totalElements).isEqualTo(5);

        assertThat(result)
                .extracting(Item::getTitle)
                .containsExactly(
                        "Test item_1 title",
                        "Test item_2 title",
                        "Test item_4 title",
                        "Test item_3 title",
                        "Test item_5 title"
                );
    }

    @Test
    void findByTitleOrDescription_shouldNotReturnItems() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
                        "something",
                        "something",
                        pageable)
                .collectList()
                .block();

        Long totalElements = itemRepository
                .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "something",
                        "something")
                .block();


        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        assertThat(totalElements).isNotNull();
        assertThat(totalElements).isZero();
    }
}