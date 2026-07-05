package ru.yandex.practicum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class MarketSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        RedirectServerAuthenticationSuccessHandler loginSuccessHandler =
                new RedirectServerAuthenticationSuccessHandler("/items");

        RedirectServerAuthenticationFailureHandler loginFailureHandler =
                new RedirectServerAuthenticationFailureHandler("/login?error");

        RedirectServerLogoutSuccessHandler logoutSuccessHandler =
                new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/login?logout"));

        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/images/**", "/css/**", "/favicon.ico").permitAll()
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/{id}").permitAll()
                        .pathMatchers(HttpMethod.GET, "/login").permitAll()
                        .pathMatchers(HttpMethod.GET, "/items/new").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/items/new").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/items", "/items/**").authenticated()
                        .pathMatchers("/cart/**", "/orders/**", "/buy").authenticated()
                        .anyExchange().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationSuccessHandler(loginSuccessHandler)
                        .authenticationFailureHandler(loginFailureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler))
                .csrf(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}