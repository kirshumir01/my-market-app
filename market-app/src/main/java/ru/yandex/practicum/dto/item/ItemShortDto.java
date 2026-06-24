package ru.yandex.practicum.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemShortDto {
    private Long id;
    private String title;
    private Long price;
    private int count;
}
