package ru.yandex.practicum.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.cart.model.CartAction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartRequest {
    private Long itemId;
    private CartAction action;
}