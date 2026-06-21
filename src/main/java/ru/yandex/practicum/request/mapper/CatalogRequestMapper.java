package ru.yandex.practicum.request.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.items.model.ItemSort;
import ru.yandex.practicum.request.dto.CatalogRequest;

@Component
@AllArgsConstructor
public class CatalogRequestMapper {

    private final RequestParamMapper requestParamMapper;

    public CatalogRequest from(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData
    ) {
        return new CatalogRequest(
                requestParamMapper.getLongParam(
                        queryParams,
                        formData,
                        "id"
                ),
                requestParamMapper.getEnumParam(
                        CartAction.class,
                        queryParams,
                        formData,
                        "action"
                ),
                requestParamMapper.getStringParam(
                        queryParams,
                        formData,
                        "search",
                        ""
                ),
                requestParamMapper.getEnumParamOrDefault(
                        ItemSort.class,
                        queryParams,
                        formData,
                        "sort",
                        ItemSort.NO
                ),
                requestParamMapper.getIntParam(
                        queryParams,
                        formData,
                        "pageNumber",
                        1
                ),
                requestParamMapper.getIntParam(
                        queryParams,
                        formData,
                        "pageSize",
                        5
                )
        );
    }
}