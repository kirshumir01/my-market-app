package ru.yandex.practicum.items.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemsPageDto {
    private List<List<ItemDto>> items;
    private PageDto paging;
}
