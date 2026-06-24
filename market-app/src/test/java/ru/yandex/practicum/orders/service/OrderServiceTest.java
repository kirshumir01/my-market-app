package ru.yandex.practicum.orders.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cache.ItemCacheService;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.dto.cache.ItemCardCacheDto;
import ru.yandex.practicum.dto.payment.PaymentRequestDto;
import ru.yandex.practicum.dto.payment.PaymentResponseDto;
import ru.yandex.practicum.dto.payment.PaymentStatus;
import ru.yandex.practicum.exception.BadRequestException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.Item;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderItem;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ItemRepository;
import ru.yandex.practicum.repository.OrderItemRepository;
import ru.yandex.practicum.repository.OrderRepository;
import ru.yandex.practicum.service.impl.OrderServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemCacheService cacheService;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Item item_1;
    private Item item_2;

    private CartItem cartItem_1;
    private CartItem cartItem_2;

    private Order order_1;
    private Order order_2;

    private OrderItem orderItem_1;
    private OrderItem orderItem_2;

    private ItemCardCacheDto itemCard_1;
    private ItemCardCacheDto itemCard_2;

    @BeforeEach
    void setUp() {
        item_1 = new Item(1L, "Test item_1 title", "Test item_1 description", null, 999L);
        item_2 = new Item(2L, "Test item_2 title", "Test item_2 description", null, 2999L);

        cartItem_1 = new CartItem(1L, 1L, 2);
        cartItem_2 = new CartItem(2L, 2L, 3);

        order_1 = new Order(1L, 999L * 2 + 2999L * 3, LocalDateTime.now());
        order_2 = new Order(2L, 0L, LocalDateTime.now().plusHours(1));

        orderItem_1 = new OrderItem(1L, 1L, 1L, 2, 999L);
        orderItem_2 = new OrderItem(2L, 1L, 2L, 3, 2999L);

        itemCard_1 = ItemCardCacheDto.builder()
                .id(item_1.getId())
                .title(item_1.getTitle())
                .description(item_1.getDescription())
                .imgPath(item_1.getImgPath())
                .price(item_1.getPrice())
                .build();

        itemCard_2 = ItemCardCacheDto.builder()
                .id(item_2.getId())
                .title(item_2.getTitle())
                .description(item_2.getDescription())
                .imgPath(item_2.getImgPath())
                .price(item_2.getPrice())
                .build();
    }

    @Test
    void getOrders_shouldReturnOrders() {
        when(orderRepository.findAllOrders())
                .thenReturn(Flux.just(order_1, order_2));

        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(1L))
                .thenReturn(Flux.just(orderItem_1, orderItem_2));

        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(2L))
                .thenReturn(Flux.empty());

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(Flux.just(item_1, item_2));

        when(itemRepository.findAllById(List.of()))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders())
                .expectNextCount(2)
                .verifyComplete();

        verify(orderRepository).findAllOrders();
        verify(orderItemRepository).findAllByOrderIdOrderByIdAsc(1L);
        verify(orderItemRepository).findAllByOrderIdOrderByIdAsc(2L);
    }

    @Test
    void getOrders_whenRepositoryReturnsEmptyFlux_shouldReturnEmptyFlux() {
        when(orderRepository.findAllOrders())
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders())
                .verifyComplete();

        verify(orderRepository).findAllOrders();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrder_whenOrderExists_shouldReturnOrder() {
        when(orderRepository.findOrderById(1L))
                .thenReturn(Mono.just(order_1));

        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(1L))
                .thenReturn(Flux.just(orderItem_1, orderItem_2));

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(Flux.just(item_1, item_2));

        StepVerifier.create(orderService.getOrder(1L))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getTotalSum()).isEqualTo(order_1.getTotalSum());
                    assertThat(result.getItems()).hasSize(2);
                })
                .verifyComplete();

        verify(orderRepository).findOrderById(1L);
        verify(orderItemRepository).findAllByOrderIdOrderByIdAsc(1L);
        verify(itemRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    void getOrder_whenOrderDoesNotExist_shouldThrowNotFoundException() {
        when(orderRepository.findOrderById(999L))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(999L))
                .expectErrorSatisfies(exception -> {
                    assertThat(exception).isInstanceOf(NotFoundException.class);
                    assertThat(exception.getMessage())
                            .isEqualTo("Order with id = 999 not found");
                })
                .verify();

        verify(orderRepository).findOrderById(999L);
        verifyNoInteractions(orderItemRepository);
        verifyNoInteractions(itemRepository);
    }

    @Test
    void createOrderFromCart_whenCartIsEmpty_shouldThrowBadRequestException() {
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.createOrderFromCart())
                .expectErrorSatisfies(exception -> {
                    assertThat(exception).isInstanceOf(BadRequestException.class);
                    assertThat(exception.getMessage()).isEqualTo("Cart is empty");
                })
                .verify();

        verify(cartItemRepository).findAll();
        verifyNoInteractions(itemRepository);
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any(Iterable.class));
        verify(cartItemRepository, never()).deleteAll();
    }

    @Test
    void createOrderFromCart_whenCartHasItems_shouldCreateOrder() {
        when(cartItemRepository.findAll())
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCard(1L))
                .thenReturn(Mono.just(itemCard_1));

        when(cacheService.getItemCard(2L))
                .thenReturn(Mono.just(itemCard_2));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.PAID,
                        BigDecimal.valueOf(999L * 2 + 2999L * 3),
                        BigDecimal.valueOf(10000),
                        "RUB",
                        "Payment completed successfully",
                        Instant.now()
                )));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    return Mono.just(order);
                });

        when(orderItemRepository.saveAll(any(Iterable.class)))
                .thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));

        when(cartItemRepository.deleteAll())
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart())
                .expectNext(1L)
                .verifyComplete();

        verify(cartItemRepository).findAll();
        verify(cacheService).getItemCard(1L);
        verify(cacheService).getItemCard(2L);
        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(any(Iterable.class));
        verify(cartItemRepository).deleteAll();

        verifyNoInteractions(itemRepository);
    }

    @Test
    void createOrderFromCart_shouldCalculateTotalCorrectly() {
        when(cartItemRepository.findAll())
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCard(1L))
                .thenReturn(Mono.just(itemCard_1));

        when(cacheService.getItemCard(2L))
                .thenReturn(Mono.just(itemCard_2));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.PAID,
                        BigDecimal.valueOf(999L * 2 + 2999L * 3),
                        BigDecimal.valueOf(10000),
                        "RUB",
                        "Payment completed successfully",
                        Instant.now()
                )));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    return Mono.just(order);
                });

        when(orderItemRepository.saveAll(any(Iterable.class)))
                .thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));

        when(cartItemRepository.deleteAll())
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart())
                .expectNext(1L)
                .verifyComplete();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        assertThat(orderCaptor.getValue().getTotalSum())
                .isEqualTo(999L * 2 + 2999L * 3);
    }

    @Test
    void createOrderFromCart_shouldClearCartAfterSuccessfulOrderCreation() {
        when(cartItemRepository.findAll())
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCard(1L))
                .thenReturn(Mono.just(itemCard_1));

        when(cacheService.getItemCard(2L))
                .thenReturn(Mono.just(itemCard_2));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.PAID,
                        BigDecimal.valueOf(999L * 2 + 2999L * 3),
                        BigDecimal.valueOf(10000),
                        "RUB",
                        "Payment completed successfully",
                        Instant.now()
                )));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    return Mono.just(order);
                });

        when(orderItemRepository.saveAll(any(Iterable.class)))
                .thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));

        when(cartItemRepository.deleteAll())
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart())
                .expectNext(1L)
                .verifyComplete();

        verify(cartItemRepository).deleteAll();
    }
}