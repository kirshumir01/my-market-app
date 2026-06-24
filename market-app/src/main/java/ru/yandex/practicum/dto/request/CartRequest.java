package ru.yandex.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.model.CartAction;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartRequest {
    private Long itemId;
    private CartAction action;
}