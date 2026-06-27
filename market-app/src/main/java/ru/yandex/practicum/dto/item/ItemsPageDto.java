package ru.yandex.practicum.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemsPageDto {
    private List<List<ItemDto>> items;
    private PageDto paging;
}
