package ru.yandex.practicum.items.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.items.model.Item;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/clear.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    @Test
    void findByTitleOrDescriptionIsCaseInsensitive_shouldReturnPageableSort() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("price").ascending());

        Page<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "test",
                        "test",
                        pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(1);

        assertThat(result.getContent())
                .extracting(Item::getTitle)
                .containsExactly(
                        "Test item_1 title",
                        "Test item_2 title",
                        "Test item_4 title",
                        "Test item_3 title",
                        "Test item_5 title"
                );

        assertThat(result.getContent())
                .extracting(Item::getPrice)
                .containsExactly(999L, 2999L, 4999L, 7999L, 11999L);
    }

    @Test
    void findByTitleOrDescription_shouldReturnSeveralItems() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "item",
                        "item",
                        pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(6);
        assertThat(result.getTotalElements()).isEqualTo(6);

        assertThat(result.getContent())
                .extracting(Item::getTitle)
                .containsExactlyInAnyOrder(
                        "Test item_1 title",
                        "Test item_2 title",
                        "Test item_4 title",
                        "Test item_3 title",
                        "Test item_5 title",
                        "Item_6 title"
                );
    }

    @Test
    void findByTitleOrDescription_shouldNotReturnItems() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Item> result = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "something",
                        "something",
                        pageable
                );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }
}