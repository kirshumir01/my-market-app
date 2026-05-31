package ru.yandex.practicum.orders.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cart.model.CartItem;
import ru.yandex.practicum.cart.repository.CartItemRepository;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.items.model.Item;
import ru.yandex.practicum.orders.dto.OrderDto;
import ru.yandex.practicum.orders.model.Order;
import ru.yandex.practicum.orders.model.OrderItem;
import ru.yandex.practicum.orders.repository.OrderItemRepository;
import ru.yandex.practicum.orders.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Item item_1;
    private Item item_2;
    private Item item_3;

    private CartItem cartItem_1;
    private CartItem cartItem_2;
    private CartItem cartItem_3;

    private Order order_1;
    private Order order_2;

    private OrderItem orderItem_1;
    private OrderItem orderItem_2;
    private OrderItem orderItem_3;

    @BeforeEach
    void setUp() {
        item_1 = new Item(1L, "Test item_1 title", "Test item_1 description", null, 999L);
        item_2 = new Item(2L, "Test item_2 title", "Test item_2 description", null, 2999L);
        item_3 = new Item(3L, "Test item_3 title", "Test item_3 description", null, 4999L);

        cartItem_1 = new CartItem(1L, item_1, 2);
        cartItem_2 = new CartItem(2L, item_2, 3);
        cartItem_3 = new CartItem(3L, item_3, 2);


        order_1 = new Order();
        order_1.setId(1L);
        order_1.setTotalSum(item_1.getPrice() * cartItem_1.getCount() + item_2.getPrice() * cartItem_2.getCount());

        order_2 = new Order();
        order_2.setId(2L);
        order_2.setTotalSum(item_3.getPrice() * cartItem_3.getCount());

        orderItem_1 = new OrderItem(1L, order_1, item_1, cartItem_1.getCount(), item_1.getPrice());
        orderItem_2 = new OrderItem(2L, order_1, item_2, cartItem_2.getCount(), item_2.getPrice());
        orderItem_3 = new OrderItem(3L, order_2, item_3, cartItem_3.getCount(), item_3.getPrice());

        order_1.setItems(List.of(orderItem_1, orderItem_2));
        order_2.setItems(List.of(orderItem_3));
    }

    @Test
    void getOrders_shouldReturnOrders() {
        when(orderRepository.findAllWithItems()).thenReturn(List.of(order_1, order_2));

        List<OrderDto> result = orderService.getOrders();

        assertThat(result).hasSize(2);

        OrderDto resultOrder = result.get(0);

        assertThat(resultOrder.getId()).isEqualTo(order_1.getId());
        assertThat(resultOrder.getTotalSum()).isEqualTo(order_1.getTotalSum());
        assertThat(resultOrder.getItems()).hasSize(2);

        verify(orderRepository).findAllWithItems();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrders_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(orderRepository.findAllWithItems()).thenReturn(List.of());

        List<OrderDto> result = orderService.getOrders();

        assertThat(result).isEmpty();

        verify(orderRepository).findAllWithItems();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrder_whenOrderExists_shouldReturnOrder() {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order_1));

        OrderDto result = orderService.getOrder(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(order_1.getId());
        assertThat(result.getTotalSum()).isEqualTo(order_1.getTotalSum());
        assertThat(result.getItems()).hasSize(2);

        verify(orderRepository).findByIdWithItems(1L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrder_whenOrderDoesNotExist_shouldThrowNotFoundException() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> orderService.getOrder(999L)
        );

        assertThat(exception.getMessage()).isEqualTo("Order with id = 999 not found");

        verify(orderRepository).findByIdWithItems(999L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrderFromCart_whenCartIsEmpty_shouldThrowBadRequestException() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of());

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.createOrderFromCart()
        );

        assertThat(exception.getMessage()).isEqualTo("Cart is empty");

        verify(cartItemRepository).findAllWithItems();

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(cartItemRepository, never()).deleteAll();

        verifyNoMoreInteractions(
                orderRepository,
                orderItemRepository,
                cartItemRepository
        );
    }

    @Test
    void createOrderFromCart_whenCartHasItems_shouldCreateOrder() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of(cartItem_1, cartItem_2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);

                    if (savedOrder.getId() == null) {
                        savedOrder.setId(1L);
                    }

                    return savedOrder;
                });

        long result = orderService.createOrderFromCart();

        assertThat(result).isEqualTo(1L);

        verify(cartItemRepository).findAllWithItems();

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(cartItemRepository).deleteAll();

        verifyNoMoreInteractions(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void createOrderFromCart_shouldCalculateTotalCorrectly() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of(cartItem_1, cartItem_2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    savedOrder.setId(1L);
                    return savedOrder;
                });

        orderService.createOrderFromCart();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        verify(orderRepository, times(2)).save(orderCaptor.capture());

        Order finalSavedOrder = orderCaptor.getAllValues().get(1);

        long expectedTotal = item_1.getPrice() * cartItem_1.getCount()
                + item_2.getPrice() * cartItem_2.getCount();

        assertThat(finalSavedOrder.getTotalSum()).isEqualTo(expectedTotal);
    }

    @Test
    void createOrderFromCart_shouldSaveOrderItemsWithCorrectPriceCountAndItem() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of(cartItem_1, cartItem_2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    savedOrder.setId(1L);
                    return savedOrder;
                });

        orderService.createOrderFromCart();

        ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);

        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());

        List<OrderItem> savedOrderItems = orderItemsCaptor.getValue();

        assertThat(savedOrderItems).hasSize(2);

        OrderItem savedOrderItem1 = savedOrderItems.stream()
                .filter(orderItem -> orderItem.getItem().getId().equals(item_1.getId()))
                .findFirst()
                .orElseThrow();

        OrderItem savedOrderItem2 = savedOrderItems.stream()
                .filter(orderItem -> orderItem.getItem().getId().equals(item_2.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(savedOrderItem1.getItem()).isEqualTo(item_1);
        assertThat(savedOrderItem1.getCount()).isEqualTo(cartItem_1.getCount());
        assertThat(savedOrderItem1.getPrice()).isEqualTo(item_1.getPrice());
        assertThat(savedOrderItem1.getOrder()).isNotNull();
        assertThat(savedOrderItem1.getOrder().getId()).isEqualTo(1L);

        assertThat(savedOrderItem2.getItem()).isEqualTo(item_2);
        assertThat(savedOrderItem2.getCount()).isEqualTo(cartItem_2.getCount());
        assertThat(savedOrderItem2.getPrice()).isEqualTo(item_2.getPrice());
        assertThat(savedOrderItem2.getOrder()).isNotNull();
        assertThat(savedOrderItem2.getOrder().getId()).isEqualTo(1L);
    }

    @Test
    void createOrderFromCart_shouldClearCartAfterSuccessfulOrderCreation() {
        when(cartItemRepository.findAllWithItems()).thenReturn(List.of(cartItem_1, cartItem_2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    savedOrder.setId(1L);
                    return savedOrder;
                });

        orderService.createOrderFromCart();

        InOrder inOrder = inOrder(
                cartItemRepository,
                orderRepository,
                orderItemRepository
        );

        inOrder.verify(cartItemRepository).findAllWithItems();
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(orderItemRepository).saveAll(anyList());
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(cartItemRepository).deleteAll();
    }
}