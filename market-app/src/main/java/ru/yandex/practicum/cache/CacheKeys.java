package ru.yandex.practicum.cache;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class CacheKeys {

    public static final String ITEM_CARD_PREFIX = "item:card:";
    public static final String ITEM_LIST_PREFIX = "item:list:";

    public static String itemCard(Long id) {
        return ITEM_CARD_PREFIX + id;
    }

    public static String itemListPattern() {
        return ITEM_LIST_PREFIX + "*";
    }
}