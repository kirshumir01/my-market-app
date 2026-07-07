package ru.yandex.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.UserRepository;
import ru.yandex.practicum.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Mono<User> getRequiredUser(String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new AccessDeniedException("User is not authenticated"));
        }

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("User with username = %s not found".formatted(username))
                ));
    }

    @Override
    public Mono<Long> getRequiredUserId(String username) {
        return getRequiredUser(username).map(User::getId);
    }

    @Override
    public Mono<Long> getOptionalUserId(String username) {
        if (username == null || username.isBlank()) {
            return Mono.empty();
        }

        return getRequiredUserId(username);
    }
}