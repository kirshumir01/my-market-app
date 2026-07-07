package ru.yandex.practicum.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.model.User;

public interface UserService {
    Mono<User> getRequiredUser(String username);

    Mono<Long> getRequiredUserId(String username);

    Mono<Long> getOptionalUserId(String username);
}
