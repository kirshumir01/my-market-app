package ru.yandex.practicum;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
public class MyMarketAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyMarketAppApplication.class, args);
    }

    @Bean
    @Profile({"dev", "docker"})
    CommandLineRunner initDatabase(
            ItemRepository itemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> clearDatabase(
                itemRepository,
                orderRepository,
                orderItemRepository,
                cartItemRepository,
                userRepository
        )
                .then(createUsers(userRepository, passwordEncoder))
                .flatMap(users ->
                        createItems(itemRepository)
                                .flatMap(items -> Mono.when(
                                        users.stream()
                                                .map(user -> Mono.when(
                                                        createOrders(
                                                                orderRepository,
                                                                orderItemRepository,
                                                                items,
                                                                user.getId()
                                                        ),
                                                        createCartItems(
                                                                cartItemRepository,
                                                                items,
                                                                user.getId()
                                                        )
                                                ))
                                                .toList()
                                ))
                )
                .block();
    }

    private Mono<Void> clearDatabase(
            ItemRepository itemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository
    ) {
        return cartItemRepository.deleteAll()
                .then(orderItemRepository.deleteAll())
                .then(orderRepository.deleteAll())
                .then(itemRepository.deleteAll())
                .then(userRepository.deleteAll());
    }

    private Mono<List<User>> createUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return userRepository.saveAll(List.of(
                new User(
                        null,
                        "user_1",
                        passwordEncoder.encode("password1"),
                        true,
                        UserRole.USER
                ),
                new User(
                        null,
                        "user_2",
                        passwordEncoder.encode("password2"),
                        true,
                        UserRole.USER
                ),
                new User(
                        null,
                        "admin",
                        passwordEncoder.encode("admin"),
                        true,
                        UserRole.ADMIN
                )
        )).collectList();
    }

    private Mono<List<Item>> createItems(ItemRepository itemRepository) {
        List<Item> items = List.of(
                new Item(null, "Test item_1 title", "Test item_1 description", "images/item_1.jpg", 999L),
                new Item(null, "Test item_2 title", "Test item_2 description", "images/item_2.jpg", 2999L),
                new Item(null, "Test item_3 title", "Test item_3 description", "images/item_3.jpg", 7999L),
                new Item(null, "Test item_4 title", "Test item_4 description", "images/item_4.jpg", 4999L),
                new Item(null, "Test item_5 title", "Test item_5 description", "images/item_5.jpg", 11999L),
                new Item(null, "Item_6 title", "Item_6 description", "images/item_6.jpg", 14999L)
        );

        return itemRepository.saveAll(items)
                .collectList();
    }

    private Mono<Void> createOrders(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            List<Item> items,
            Long userId
    ) {
        int ordersCount = 2;

        return Flux.range(0, ordersCount)
                .concatMap(orderIndex -> createRandomOrder(
                        orderRepository,
                        orderItemRepository,
                        items,
                        userId,
                        orderIndex
                ))
                .then();
    }

    private Mono<Void> createRandomOrder(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            List<Item> items,
            Long userId,
            int orderIndex
    ) {
        int itemsInOrder = ThreadLocalRandom.current().nextInt(1, 4);

        List<Item> selectedItems = new ArrayList<>(items);
        Collections.shuffle(selectedItems);
        selectedItems = selectedItems.stream()
                .limit(itemsInOrder)
                .toList();

        List<OrderItem> orderItems = selectedItems.stream()
                .map(item -> {
                    int count = ThreadLocalRandom.current().nextInt(1, 11);

                    return new OrderItem(
                            null,
                            null,
                            item.getId(),
                            count,
                            item.getPrice()
                    );
                })
                .toList();

        long totalSum = orderItems.stream()
                .mapToLong(orderItem -> orderItem.getPrice() * orderItem.getCount())
                .sum();

        return orderRepository.save(
                new Order(
                        null,
                        userId,
                        totalSum,
                        LocalDateTime.now().plusHours(orderIndex)
                )
        ).flatMap(order -> orderItemRepository.saveAll(
                                orderItems.stream()
                                        .map(orderItem -> new OrderItem(
                                                null,
                                                order.getId(),
                                                orderItem.getItemId(),
                                                orderItem.getCount(),
                                                orderItem.getPrice()
                                        ))
                                        .toList()
                        )
                        .then()
        );
    }

    private Mono<Void> createCartItems(
            CartItemRepository cartItemRepository,
            List<Item> items,
            Long userId
    ) {
        int itemsInCart = ThreadLocalRandom.current().nextInt(1, 4);

        List<Item> selectedItems = new ArrayList<>(items);
        Collections.shuffle(selectedItems);

        List<CartItem> cartItems = selectedItems.stream()
                .limit(itemsInCart)
                .map(item -> new CartItem(
                        null,
                        userId,
                        item.getId(),
                        ThreadLocalRandom.current().nextInt(1, 3)
                ))
                .toList();

        return cartItemRepository.saveAll(cartItems)
                .then();
    }
}