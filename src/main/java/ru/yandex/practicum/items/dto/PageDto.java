package ru.yandex.practicum.items.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PageDto {
    private int pageSize;
    private int pageNumber;
    private boolean hasPrevious;
    private boolean hasNext;
}
