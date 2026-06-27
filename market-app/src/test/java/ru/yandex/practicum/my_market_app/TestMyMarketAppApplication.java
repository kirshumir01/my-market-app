package ru.yandex.practicum.my_market_app;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;
import ru.yandex.practicum.MyMarketAppApplication;


public class TestMyMarketAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(MyMarketAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}