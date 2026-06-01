package ru.yandex.practicum.cart.dto;

import lombok.*;
import ru.yandex.practicum.items.dto.ItemDto;

import java.util.List;

@Data
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartDto {
    private List<ItemDto> items;
    private Long total;
}
