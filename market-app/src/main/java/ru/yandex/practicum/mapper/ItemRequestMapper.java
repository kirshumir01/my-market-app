package ru.yandex.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import ru.yandex.practicum.model.CartAction;

@Component
@RequiredArgsConstructor
public class ItemRequestMapper {

    private final RequestParamMapper requestParamMapper;

    public CartAction getAction(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData
    ) {
        return requestParamMapper.getEnumParam(
                CartAction.class,
                queryParams,
                formData,
                "action"
        );
    }
}