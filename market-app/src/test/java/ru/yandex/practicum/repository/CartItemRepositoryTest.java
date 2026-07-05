package ru.yandex.practicum.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.config.TestDataConfiguration;
import ru.yandex.practicum.model.CartItem;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class CartItemRepositoryTest extends TestDataConfiguration {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;

    @Autowired
    CartItemRepository cartItemRepository;

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @DisplayName("findAllByUserId(userId) -> returns user's cart items")
    void findAllByUserId_shouldReturnUserCartItems() {
        List<CartItem> result = cartItemRepository.findAllByUserId(USER_ID)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(CartItem::getUserId)
                .containsOnly(USER_ID);

        assertThat(result)
                .extracting(CartItem::getItemId)
                .containsExactlyInAnyOrder(1L, 2L);

        assertThat(result)
                .extracting(CartItem::getCount)
                .containsExactlyInAnyOrder(2, 5);
    }

    @Test
    @DisplayName("findAllByUserId(userId) -> returns empty when user has no cart items")
    void findAllByUserId_shouldReturnEmptyWhenUserHasNoCartItems() {
        List<CartItem> result = cartItemRepository.findAllByUserId(999L)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserIdAndItemIdIn(userId, itemIds) -> returns matching user's cart items")
    void findAllByUserIdAndItemIdIn_shouldReturnUserCartItems() {
        List<Long> itemIds = List.of(1L, 2L, 3L);

        List<CartItem> result = cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, itemIds)
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(CartItem::getUserId)
                .containsOnly(USER_ID);

        assertThat(result)
                .extracting(CartItem::getItemId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("findAllByUserIdAndItemIdIn(userId, itemIds) -> does not return other user's items")
    void findAllByUserIdAndItemIdIn_shouldNotReturnOtherUserItems() {
        List<CartItem> result = cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(3L))
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndItemId(userId, itemId) -> returns user's cart item")
    void findByUserIdAndItemId_shouldReturnUserCartItem() {
        CartItem result = cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)
                .block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getItemId()).isEqualTo(1L);
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByUserIdAndItemId(userId, itemId) -> returns empty for other user's item")
    void findByUserIdAndItemId_shouldReturnEmptyForOtherUserItem() {
        CartItem result = cartItemRepository.findByUserIdAndItemId(USER_ID, 3L)
                .block();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("deleteAllByUserId(userId) -> deletes only user's cart items")
    void deleteAllByUserId_shouldDeleteOnlyUserCartItems() {
        cartItemRepository.deleteAllByUserId(USER_ID)
                .block();

        List<CartItem> userCartItems = cartItemRepository.findAllByUserId(USER_ID)
                .collectList()
                .block();

        List<CartItem> otherUserCartItems = cartItemRepository.findAllByUserId(OTHER_USER_ID)
                .collectList()
                .block();

        assertThat(userCartItems).isNotNull();
        assertThat(userCartItems).isEmpty();

        assertThat(otherUserCartItems).isNotNull();
        assertThat(otherUserCartItems).hasSize(1);
        assertThat(otherUserCartItems.getFirst().getItemId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findAll() -> returns all cart items for all users")
    void findAll_shouldReturnAllCartItems() {
        List<CartItem> result = cartItemRepository.findAll()
                .collectList()
                .block();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        assertThat(result)
                .extracting(CartItem::getUserId)
                .containsExactlyInAnyOrder(1L, 1L, 2L);

        assertThat(result)
                .extracting(CartItem::getItemId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);

        assertThat(result)
                .extracting(CartItem::getCount)
                .containsExactlyInAnyOrder(2, 5, 1);
    }
}