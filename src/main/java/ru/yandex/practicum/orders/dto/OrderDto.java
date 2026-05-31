package ru.yandex.practicum.orders.dto;

import lombok.*;
import ru.yandex.practicum.items.dto.ItemShortDto;

import java.util.List;

@Data
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private Long id;
    private List<ItemShortDto> items;
    private Long totalSum;
}
