package ru.yandex.practicum.items.dto;

import lombok.*;

@Data
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemShortDto {
    private Long id;
    private String title;
    private Long price;
    private int count;
}
