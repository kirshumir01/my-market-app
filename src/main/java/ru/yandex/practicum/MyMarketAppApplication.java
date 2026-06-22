package ru.yandex.practicum;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;
import ru.yandex.practicum.orders.model.Order;
import ru.yandex.practicum.orders.model.OrderItem;
import ru.yandex.practicum.orders.repository.OrderItemRepository;
import ru.yandex.practicum.orders.repository.OrderRepository;

import java.util.List;

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
            CartItemRepository cartItemRepository
    ) {
        return args -> clearDatabase(
                itemRepository,
                orderRepository,
                orderItemRepository,
                cartItemRepository
        )
                .then(createItems(itemRepository))
                .flatMap(items -> createOrders(orderRepository, orderItemRepository, items)
                        .then(createCartItems(cartItemRepository, items)))
                .block();
    }

    private Mono<Void> clearDatabase(
            ItemRepository itemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository

    ) {
        return cartItemRepository.deleteAll()
                .then(orderItemRepository.deleteAll())
                .then(orderRepository.deleteAll())
                .then(itemRepository.deleteAll());
    }

    private Mono<List<Item>> createItems(ItemRepository itemRepository) {
        return Flux.just(
                new Item(null, "Test item_1 title", "Test item_1 description", "images/item_1.jpg", 999L),
                new Item(null, "Test item_2 title", "Test item_2 description", "images/item_2.jpg", 2999L),
                new Item(null, "Test item_3 title", "Test item_3 description", "images/item_3.jpg", 7999L),
                new Item(null, "Test item_4 title", "Test item_4 description", "images/item_4.jpg", 4999L),
                new Item(null, "Test item_5 title", "Test item_5 description", "images/item_5.jpg", 11999L),
                new Item(null, "Item_6 title", "Item_6 description", "images/item_6.jpg", 14999L)
        )
                .concatMap(itemRepository::save)
                .collectList();
    }

    private Mono<Void> createOrders(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            List<Item> items

    ) {
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        Item item3 = items.get(2);

        Mono<Order> order1Mono = orderRepository.save(new Order(null, 4997L));
        Mono<Order> order2Mono = orderRepository.save(new Order(null, 23997L));

        return order1Mono
                .flatMap(order1 -> Flux.just(
                        new OrderItem(null, order1.getId(), item1.getId(), 2, 999L),
                        new OrderItem(null, order1.getId(), item2.getId(), 1, 2999L)
                )
                        .concatMap(orderItemRepository::save)
                        .then())
                .then(order2Mono.flatMap(order2 -> orderItemRepository.save(
                        new OrderItem(null, order2.getId(), item3.getId(), 3, 7999L)
                )))
                .then();
    }

    private Mono<Void> createCartItems(
            CartItemRepository cartItemRepository,
            List<Item> items
    ) {
        Item item1 = items.get(0);
        Item item2 = items.get(1);

        return Flux.just(
                new CartItem(null, item1.getId(), 2),
                new CartItem(null, item2.getId(), 5)
                )
                .concatMap(cartItemRepository::save)
                .then();
    }
}