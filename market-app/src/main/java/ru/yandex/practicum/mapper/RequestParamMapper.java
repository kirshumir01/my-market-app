package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import ru.yandex.practicum.exception.BadRequestException;

@Component
public class RequestParamMapper {

    public String getStringParam(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name,
            String defaultValue
    ) {
        String value = firstValue(queryParams, formData, name);

        return value == null
                ? defaultValue
                : value;
    }

    public int getIntParam(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name,
            int defaultValue
    ) {
        String value = firstValue(queryParams, formData, name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "Parameter '%s' must be an integer".formatted(name)
            );
        }
    }

    public Long getLongParam(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String... names
    ) {
        for (String name : names) {
            String value = firstValue(queryParams, formData, name);

            if (value != null && !value.isBlank()) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new BadRequestException(
                            "Parameter '%s' must be a number".formatted(name)
                    );
                }
            }
        }

        throw new BadRequestException("Required parameter 'id' is missing");
    }

    public <T extends Enum<T>> T getEnumParam(
            Class<T> enumClass,
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name
    ) {
        String value = firstValue(queryParams, formData, name);

        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    "Required parameter '%s' is missing".formatted(name)
            );
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid value '%s' for parameter '%s'"
                            .formatted(value, name)
            );
        }
    }

    public <T extends Enum<T>> T getEnumParamOrDefault(
            Class<T> enumClass,
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name,
            T defaultValue
    ) {
        String value = firstValue(queryParams, formData, name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid value '%s' for parameter '%s'"
                            .formatted(value, name)
            );
        }
    }

    private String firstValue(
            MultiValueMap<String, String> queryParams,
            MultiValueMap<String, String> formData,
            String name
    ) {
        String queryValue = queryParams.getFirst(name);

        if (queryValue != null) {
            return queryValue;
        }

        return formData.getFirst(name);
    }
}
