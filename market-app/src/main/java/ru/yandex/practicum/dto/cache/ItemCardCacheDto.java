package ru.yandex.practicum.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCardCacheDto {
    private Long id;
    private String title;
    private String description;
    private long price;
    private String imgPath;
}