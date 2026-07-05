package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;
import ru.yandex.practicum.service.impl.OrderServiceImpl;
import ru.yandex.practicum.util.TestDataFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String USERNAME = TestDataFactory.USERNAME;
    private static final String OTHER_USERNAME = TestDataFactory.OTHER_USERNAME;
    private static final long USER_ID = TestDataFactory.USER_ID;
    private static final long OTHER_USER_ID = TestDataFactory.OTHER_USER_ID;

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderItemRepository orderItemRepository;
    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    ItemRepository itemRepository;
    @Mock
    ItemCacheService cacheService;
    @Mock
    PaymentClient paymentClient;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private User otherUser;

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
        user = TestDataFactory.user();
        otherUser = TestDataFactory.otherUser();

        item_1 = TestDataFactory.item1();
        item_2 = TestDataFactory.item2();

        cartItem_1 = TestDataFactory.cartItem1();
        cartItem_2 = TestDataFactory.cartItem2();

        order_1 = TestDataFactory.order1();
        order_2 = TestDataFactory.order2();

        orderItem_1 = TestDataFactory.orderItem1();
        orderItem_2 = TestDataFactory.orderItem2();

        itemCard_1 = TestDataFactory.itemCard1();
        itemCard_2 = TestDataFactory.itemCard2();
    }

    @Test
    @DisplayName("getOrders(username) -> returns user's orders")
    void getOrders_shouldReturnUserOrders() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(orderRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                .thenReturn(Flux.just(order_1, order_2));

        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(1L))
                .thenReturn(Flux.just(orderItem_1, orderItem_2));
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(2L))
                .thenReturn(Flux.empty());

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(Flux.just(item_1, item_2));
        when(itemRepository.findAllById(List.of()))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders(USERNAME))
                .expectNextCount(2)
                .verifyComplete();

        verify(userRepository).findByUsername(USERNAME);
        verify(orderRepository).findAllByUserIdOrderByIdAsc(USER_ID);
        verify(orderItemRepository).findAllByOrderIdOrderByIdAsc(1L);
        verify(orderItemRepository).findAllByOrderIdOrderByIdAsc(2L);
    }

    @Test
    @DisplayName("getOrders(username) -> returns empty Flux when user has no orders")
    void getOrders_whenUserHasNoOrders_shouldReturnEmptyFlux() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(orderRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders(USERNAME))
                .verifyComplete();

        verify(userRepository).findByUsername(USERNAME);
        verify(orderRepository).findAllByUserIdOrderByIdAsc(USER_ID);
        verifyNoInteractions(orderItemRepository, cacheService);
    }

    @Test
    @DisplayName("getOrder(username, id) -> returns order when it belongs to user")
    void getOrder_whenOrderExistsAndBelongsToUser_shouldReturnOrder() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(orderRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Mono.just(order_1));

        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(1L))
                .thenReturn(Flux.just(orderItem_1, orderItem_2));

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(Flux.just(item_1, item_2));

        StepVerifier.create(orderService.getOrder(USERNAME, 1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getTotalSum()).isEqualTo(order_1.getTotalSum());
                    assertThat(result.getItems()).hasSize(2);
                })
                .verifyComplete();

        verify(userRepository).findByUsername(USERNAME);
        verify(orderRepository).findByIdAndUserId(1L, USER_ID);
        verify(orderItemRepository).findAllByOrderIdOrderByIdAsc(1L);
        verify(itemRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("getOrder(username, id) -> throws NotFoundException when order does not belong to user")
    void getOrder_whenOrderBelongsToAnotherUser_shouldThrowNotFoundException() {
        when(userRepository.findByUsername(OTHER_USERNAME)).thenReturn(Mono.just(otherUser));
        when(orderRepository.findByIdAndUserId(1L, OTHER_USER_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(OTHER_USERNAME, 1L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage()).isEqualTo("Order with id = 1 not found");
                })
                .verify();

        verify(userRepository).findByUsername(OTHER_USERNAME);
        verify(orderRepository).findByIdAndUserId(1L, OTHER_USER_ID);
        verifyNoInteractions(orderItemRepository, cacheService);
    }

    @Test
    @DisplayName("getOrders(other-user) -> does not return user_id=1 orders")
    void getOrders_whenOtherUser_shouldNotReturnFirstUserOrders() {
        when(userRepository.findByUsername(OTHER_USERNAME)).thenReturn(Mono.just(otherUser));
        when(orderRepository.findAllByUserIdOrderByIdAsc(OTHER_USER_ID))
                .thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrders(OTHER_USERNAME))
                .verifyComplete();

        verify(userRepository).findByUsername(OTHER_USERNAME);
        verify(orderRepository).findAllByUserIdOrderByIdAsc(OTHER_USER_ID);
        verify(orderRepository, never()).findAllByUserIdOrderByIdAsc(USER_ID);
        verifyNoInteractions(orderItemRepository, cacheService);
    }

    @Test
    @DisplayName("getOrder(null, id) -> throws NotFoundException for anonymous user")
    void getOrder_whenAnonymousUser_shouldThrowException() {
        when(userRepository.findByUsername(null)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(null, 1L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(NotFoundException.class);
                    assertThat(error.getMessage()).isEqualTo("User with username = null not found");
                })
                .verify();

        verify(userRepository).findByUsername(null);
        verifyNoInteractions(orderRepository, orderItemRepository, cacheService);
    }

    @Test
    @DisplayName("createOrderFromCart(username) -> throws BadRequestException when cart is empty")
    void createOrderFromCart_whenCartIsEmpty_shouldThrowBadRequestException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(cartItemRepository.findAllByUserId(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.createOrderFromCart(USERNAME))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BadRequestException.class);
                    assertThat(error.getMessage()).isEqualTo("Cart is empty");
                })
                .verify();

        verify(userRepository).findByUsername(USERNAME);
        verify(cartItemRepository).findAllByUserId(USER_ID);
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any(Iterable.class));
        verify(cartItemRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    @DisplayName("createOrderFromCart(username) -> creates order, saves items and clears user's cart")
    void createOrderFromCart_whenCartHasItems_shouldCreateOrder() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(cartItemRepository.findAllByUserId(USER_ID))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard_1));
        when(cacheService.getItemCardCached(2L)).thenReturn(Mono.just(itemCard_2));

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

        when(cartItemRepository.deleteAllByUserId(USER_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart(USERNAME))
                .expectNext(1L)
                .verifyComplete();

        verify(userRepository).findByUsername(USERNAME);
        verify(cartItemRepository).findAllByUserId(USER_ID);
        verify(cacheService).getItemCardCached(1L);
        verify(cacheService).getItemCardCached(2L);
        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(any(Iterable.class));
        verify(cartItemRepository).deleteAllByUserId(USER_ID);
        verify(cartItemRepository, never()).deleteAll();
        verifyNoInteractions(itemRepository);
    }

    @Test
    @DisplayName("createOrderFromCart(username) -> calculates total order amount correctly")
    void createOrderFromCart_shouldCalculateTotalCorrectly() {
        long totalSum = 999L * 2 + 2999L * 3;

        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(cartItemRepository.findAllByUserId(USER_ID))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard_1));
        when(cacheService.getItemCardCached(2L)).thenReturn(Mono.just(itemCard_2));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.PAID,
                        BigDecimal.valueOf(totalSum),
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

        when(cartItemRepository.deleteAllByUserId(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrderFromCart(USERNAME))
                .expectNext(1L)
                .verifyComplete();

        ArgumentCaptor<PaymentRequestDto> paymentCaptor =
                ArgumentCaptor.forClass(PaymentRequestDto.class);

        verify(paymentClient).makePayment(paymentCaptor.capture());

        assertThat(paymentCaptor.getValue().getOrderId()).isNull();
        assertThat(paymentCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(paymentCaptor.getValue().getAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(totalSum));
        assertThat(paymentCaptor.getValue().getCurrency()).isEqualTo("RUB");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        assertThat(orderCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(orderCaptor.getValue().getTotalSum()).isEqualTo(totalSum);
    }

    @Test
    @DisplayName("createOrderFromCart(username) -> throws BadRequestException when payment failed")
    void createOrderFromCart_whenPaymentFailed_shouldThrowBadRequestException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(cartItemRepository.findAllByUserId(USER_ID))
                .thenReturn(Flux.just(cartItem_1, cartItem_2));

        when(cacheService.getItemCardCached(1L)).thenReturn(Mono.just(itemCard_1));
        when(cacheService.getItemCardCached(2L)).thenReturn(Mono.just(itemCard_2));

        when(paymentClient.makePayment(any(PaymentRequestDto.class)))
                .thenReturn(Mono.just(new PaymentResponseDto(
                        null,
                        PaymentStatus.FAILED,
                        BigDecimal.valueOf(999L * 2 + 2999L * 3),
                        BigDecimal.valueOf(100),
                        "RUB",
                        "Not enough money on balance",
                        Instant.now()
                )));

        StepVerifier.create(orderService.createOrderFromCart(USERNAME))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BadRequestException.class);
                    assertThat(error.getMessage()).isEqualTo("Not enough money on balance");
                })
                .verify();

        verify(paymentClient).makePayment(any(PaymentRequestDto.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any(Iterable.class));
        verify(cartItemRepository, never()).deleteAllByUserId(anyLong());
    }
}