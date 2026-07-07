package ru.yandex.practicum.util;

import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.payment.BalanceResponseDto;
import ru.yandex.practicum.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestDataFactory {

    public static final Long USER_ID = 1L;
    public static final Long OTHER_USER_ID = 2L;

    public static final String USERNAME = "user";
    public static final String OTHER_USERNAME = "otherUser";

    private TestDataFactory() {
    }

    public static User user() {
        return new User(USER_ID, USERNAME, "password", true, UserRole.USER);
    }

    public static User otherUser() {
        return new User(OTHER_USER_ID, OTHER_USERNAME, "password", true, UserRole.USER);
    }

    public static Item item1() {
        return new Item(1L, "Test item_1 title", "Test item_1 description", null, 999L);
    }

    public static Item item2() {
        return new Item(2L, "Test item_2 title", "Test item_2 description", null, 2999L);
    }

    public static Item item3() {
        return new Item(3L, "Test item_3 title", "Test item_3 description", null, 7999L);
    }

    public static Item item4() {
        return new Item(4L, "Test item_4 title", "Test item_4 description", null, 7999L);
    }

    public static CartItem cartItem1() {
        return new CartItem(1L, USER_ID, item1().getId(), 2);
    }

    public static CartItem cartItem2() {
        return new CartItem(2L, USER_ID, item2().getId(), 3);
    }

    public static Order order1() {
        return new Order(
                1L,
                USER_ID,
                999L * 2 + 2999L * 3,
                LocalDateTime.now()
        );
    }

    public static Order order2() {
        return new Order(
                2L,
                USER_ID,
                0L,
                LocalDateTime.now().plusHours(1)
        );
    }

    public static OrderItem orderItem1() {
        return new OrderItem(1L, 1L, 1L, 2, 999L);
    }

    public static OrderItem orderItem2() {
        return new OrderItem(2L, 1L, 2L, 3, 2999L);
    }

    public static ItemCardCacheDto itemCard1() {
        Item item = item1();

        return toItemCard(item);
    }

    public static ItemCardCacheDto itemCard2() {
        Item item = item2();

        return toItemCard(item);
    }

    public static ItemCardCacheDto toItemCard(Item item) {
        return ItemCardCacheDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .imgPath(item.getImgPath())
                .price(item.getPrice())
                .build();
    }

    public static BalanceResponseDto balanceDto() {
        return new BalanceResponseDto(
                BigDecimal.valueOf(10000),
                "RUB");
    }
}