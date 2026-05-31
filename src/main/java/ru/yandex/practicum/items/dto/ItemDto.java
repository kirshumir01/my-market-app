package ru.yandex.practicum.items.dto;

import lombok.*;

@Data
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
    private int count;
}
