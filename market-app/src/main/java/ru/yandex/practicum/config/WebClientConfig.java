package ru.yandex.practicum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient paymentServiceWebClient(
            @Value("${payment-service.base-url}") String paymentServiceBaseUrl
    ) {
        return WebClient.builder()
                .baseUrl(paymentServiceBaseUrl)
                .build();
    }
}
