package ru.yandex.practicum.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.Item;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends TestDataConfiguration {

    @Autowired
    ItemRepository itemRepository;

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("Search items -> returns pageable results sorted by price")
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
    @DisplayName("Search items -> returns matching items")
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
    @DisplayName("Search items -> returns empty when nothing matches")
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

    @Test
    @DisplayName("findAllBy(pageable) -> returns pageable items sorted by price")
    void findAllBy_shouldReturnPageableItemsSortedByPrice() {
        Pageable pageable = PageRequest.of(0, 3, Sort.by("price").ascending());

        List<Item> result = itemRepository.findAllBy(pageable)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        assertThat(result)
                .extracting(Item::getTitle)
                .containsExactly(
                        "Test item_1 title",
                        "Test item_2 title",
                        "Test item_4 title"
                );

        assertThat(result)
                .extracting(Item::getPrice)
                .containsExactly(999L, 2999L, 4999L);
    }
}