package ru.yandex.practicum.utils;

import org.springframework.security.core.Authentication;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public static String getUsername(Authentication authentication) {
        return authentication != null
                ? authentication.getName()
                : null;
    }
}