package ru.yandex.practicum.request.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import ru.yandex.practicum.cart.model.CartAction;
import ru.yandex.practicum.request.dto.CartRequest;

@Component
@AllArgsConstructor
public class CartRequestMapper {

    private final RequestParamMapper requestParamMapper;

    public CartRequest from(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData
    ) {
        return new CartRequest(
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
                )
        );
    }
}
