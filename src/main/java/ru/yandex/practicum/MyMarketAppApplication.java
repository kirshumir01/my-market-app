package ru.yandex.practicum;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.items.repository.ItemRepository;
import ru.yandex.practicum.orders.model.Order;
import ru.yandex.practicum.orders.model.OrderItem;
import ru.yandex.practicum.orders.repository.OrderItemRepository;
import ru.yandex.practicum.orders.repository.OrderRepository;

import java.util.ArrayList;
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
        return args -> {
            cartItemRepository.deleteAll();
            orderItemRepository.deleteAll();
            orderRepository.deleteAll();
            itemRepository.deleteAll();

            List<Item> items = createItems(itemRepository);

            createOrders(orderRepository, orderItemRepository, items);
            createCartItems(cartItemRepository, items);
        };
    }

    private List<Item> createItems(ItemRepository itemRepository) {
        return List.of(
                itemRepository.save(new Item(null, "Test item_1 title", "Test item_1 description", "images/item_1.jpg", 999L)),
                itemRepository.save(new Item(null, "Test item_2 title", "Test item_2 description", "images/item_2.jpg", 2999L)),
                itemRepository.save(new Item(null, "Test item_3 title", "Test item_3 description", "images/item_3.jpg", 7999L)),
                itemRepository.save(new Item(null, "Test item_4 title", "Test item_4 description", "images/item_4.jpg", 4999L)),
                itemRepository.save(new Item(null, "Test item_5 title", "Test item_5 description", "images/item_5.jpg", 11999L)),
                itemRepository.save(new Item(null, "Item_6 title", "Item_6 description", "images/item_6.jpg", 14999L))
        );
    }

    private void createOrders(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            List<Item> items

    ) {
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        Item item3 = items.get(2);

        Order order1 = orderRepository.save(new Order(null, new ArrayList<>(), 4997L));
        Order order2 = orderRepository.save(new Order(null, new ArrayList<>(), 23997L));

        orderItemRepository.save(new OrderItem(null, order1, item1, 2, 999L));
        orderItemRepository.save(new OrderItem(null, order1, item2, 1, 2999L));
        orderItemRepository.save(new OrderItem(null, order2, item3, 3, 7999L));
    }

    private void createCartItems(
            CartItemRepository cartItemRepository,
            List<Item> items
    ) {
        Item item1 = items.get(0);
        Item item2 = items.get(1);

        cartItemRepository.save(new CartItem(null, item1, 2));
        cartItemRepository.save(new CartItem(null, item2, 5));
    }
}