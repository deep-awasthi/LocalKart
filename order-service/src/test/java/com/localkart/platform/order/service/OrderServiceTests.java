package com.localkart.platform.order.service;

import com.localkart.platform.order.client.InventoryServiceClient;
import com.localkart.platform.order.domain.Order;
import com.localkart.platform.order.domain.OrderItem;
import com.localkart.platform.order.domain.OrderStatus;
import com.localkart.platform.order.repository.OrderRepository;
import com.localkart.platform.order.service.impl.OrderServiceImpl;
import com.localkart.platform.order.web.dto.OrderItemRequest;
import com.localkart.platform.order.web.dto.OrderRequest;
import com.localkart.platform.order.web.dto.ReservationRequest;
import com.localkart.platform.order.messaging.event.OrderEvent;
import com.localkart.platform.order.messaging.publisher.OrderEventPublisher;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testCreateOrderSuccess() {
        OrderRequest request = OrderRequest.builder()
                .items(Arrays.asList(
                        new OrderItemRequest("SKU-1", 2, BigDecimal.valueOf(10.00)),
                        new OrderItemRequest("SKU-2", 1, BigDecimal.valueOf(25.00))
                ))
                .build();

        Order order = Order.builder()
                .orderNumber("ORD-123456")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(45.00))
                .build();

        order.addItem(OrderItem.builder().productSku("SKU-1").quantity(2).price(BigDecimal.valueOf(10.00)).build());
        order.addItem(OrderItem.builder().productSku("SKU-2").quantity(1).price(BigDecimal.valueOf(25.00)).build());

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(inventoryServiceClient.reserveStock(any(ReservationRequest.class))).thenReturn("Reserved");
        doNothing().when(orderEventPublisher).publishOrderEvent(any(OrderEvent.class));

        Order result = orderService.createOrder(request, "john");

        assertNotNull(result);
        assertEquals("ORD-123456", result.getOrderNumber());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(inventoryServiceClient, times(2)).reserveStock(any(ReservationRequest.class));
        verify(orderEventPublisher, times(1)).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void testCreateOrderStockReservationFailureSagaRollback() {
        OrderRequest request = OrderRequest.builder()
                .items(Arrays.asList(
                        new OrderItemRequest("SKU-1", 2, BigDecimal.valueOf(10.00)),
                        new OrderItemRequest("SKU-2", 1, BigDecimal.valueOf(25.00))
                ))
                .build();

        Order order = Order.builder()
                .orderNumber("ORD-123456")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(45.00))
                .build();

        OrderItem item1 = OrderItem.builder().productSku("SKU-1").quantity(2).price(BigDecimal.valueOf(10.00)).build();
        OrderItem item2 = OrderItem.builder().productSku("SKU-2").quantity(1).price(BigDecimal.valueOf(25.00)).build();
        order.addItem(item1);
        order.addItem(item2);

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        // SKU-1 succeeds, SKU-2 fails
        when(inventoryServiceClient.reserveStock(eq(new ReservationRequest("SKU-1", 2)))).thenReturn("Reserved");
        when(inventoryServiceClient.reserveStock(eq(new ReservationRequest("SKU-2", 1)))).thenThrow(new RuntimeException("Out of stock"));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.createOrder(request, "john"));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        // Verify Saga compensating action was triggered for the successfully reserved SKU-1
        verify(inventoryServiceClient, times(1)).releaseStock(eq(new ReservationRequest("SKU-1", 2)));
        verify(orderEventPublisher, never()).publishOrderEvent(any());
    }

    @Test
    void testGetOrderByNumberSuccess() {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(10.00))
                .build();

        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));

        Order result = orderService.getOrderByNumber("ORD-123", "john");

        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderNumber());
    }

    @Test
    void testGetOrderByNumberAccessDenied() {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(10.00))
                .build();

        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.getOrderByNumber("ORD-123", "jane"));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void testProcessPaymentResultSuccess() {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(10.00))
                .build();

        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doNothing().when(orderEventPublisher).publishOrderEvent(any(OrderEvent.class));

        orderService.processPaymentResult("ORD-123", "SUCCESS");

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderEventPublisher, times(1)).publishOrderEvent(any(OrderEvent.class));
        verify(inventoryServiceClient, never()).releaseStock(any());
    }

    @Test
    void testProcessPaymentResultFailureSagaCompensating() {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(10.00))
                .build();
        order.addItem(OrderItem.builder().productSku("SKU-1").quantity(2).price(BigDecimal.valueOf(5.00)).build());

        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(inventoryServiceClient.releaseStock(any(ReservationRequest.class))).thenReturn("Released");
        doNothing().when(orderEventPublisher).publishOrderEvent(any(OrderEvent.class));

        orderService.processPaymentResult("ORD-123", "FAILED");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(inventoryServiceClient, times(1)).releaseStock(eq(new ReservationRequest("SKU-1", 2)));
        verify(orderEventPublisher, times(1)).publishOrderEvent(any(OrderEvent.class));
    }
}
