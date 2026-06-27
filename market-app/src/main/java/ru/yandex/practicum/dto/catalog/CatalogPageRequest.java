package ru.yandex.practicum.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.model.ItemSort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogPageRequest {
    private String search;
    private ItemSort sort;
    private int pageNumber;
    private int pageSize;
    private Pageable pageable;
}
