package ru.yandex.practicum.orders.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.mapper.OrderMapper;
import ru.yandex.practicum.orders.model.Order;
import ru.yandex.practicum.orders.model.OrderItem;
import ru.yandex.practicum.orders.repository.OrderItemRepository;
import ru.yandex.practicum.orders.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrders() {
        List<Order> orders = orderRepository.findAllWithItems();
        return OrderMapper.toOrderDtoList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(long orderId) {
        Optional<Order> order = orderRepository.findByIdWithItems(orderId);

        if (order.isEmpty()) {
            throw new NotFoundException(String.format("Order with id = %d not found", orderId));
        }

        return OrderMapper.toOrderDto(order.get());
    }

    @Override
    @Transactional
    public long createOrderFromCart() {
        List<CartItem> cartItemList = cartItemRepository.findAllWithItems();

        if (cartItemList.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = new Order();
        order.setTotalSum(0L);
        order.setItems(new ArrayList<>());

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();

        cartItemList.forEach(cartItem -> {
                Item item = cartItem.getItem();

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setItem(item);
                orderItem.setCount(cartItem.getCount());
                orderItem.setPrice(item.getPrice());

                orderItems.add(orderItem);
        });

        long total = cartItemList.stream()
                .mapToLong(cartItem -> cartItem.getItem().getPrice() * cartItem.getCount())
                .sum();

        orderItemRepository.saveAll(orderItems);

        savedOrder.setTotalSum(total);
        orderRepository.save(savedOrder);

        cartItemRepository.deleteAll();

        return savedOrder.getId();
    }
}
