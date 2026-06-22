package ru.yandex.practicum.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDto {
    private Long itemId;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
    private Integer count;
    private Long totalPrice;
}