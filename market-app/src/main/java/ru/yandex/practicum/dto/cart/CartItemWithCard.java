package ru.yandex.practicum.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.model.CartItem;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemWithCard {
    private CartItem cartItem;
    private ItemCardCacheDto itemCard;
}