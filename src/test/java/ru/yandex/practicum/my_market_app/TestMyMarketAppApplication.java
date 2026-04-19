package ru.yandex.practicum.my_market_app;

import org.springframework.boot.SpringApplication;

public class TestMyMarketAppApplication {

	public static void main(String[] args) {
		SpringApplication.from(MyMarketAppApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
