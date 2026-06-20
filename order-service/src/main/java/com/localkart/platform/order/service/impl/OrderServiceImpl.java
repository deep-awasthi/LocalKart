package com.localkart.platform.order.service.impl;

import com.localkart.platform.order.client.InventoryServiceClient;
import com.localkart.platform.order.domain.Order;
import com.localkart.platform.order.domain.OrderItem;
import com.localkart.platform.order.domain.OrderStatus;
import com.localkart.platform.order.repository.OrderRepository;
import com.localkart.platform.order.service.OrderService;
import com.localkart.platform.order.web.dto.OrderRequest;
import com.localkart.platform.order.web.dto.ReservationRequest;
import com.localkart.platform.order.messaging.event.OrderEvent;
import com.localkart.platform.order.messaging.event.OrderItemEvent;
import com.localkart.platform.order.messaging.publisher.OrderEventPublisher;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderEventPublisher orderEventPublisher;

    @Override
    @Transactional
    public Order createOrder(OrderRequest request, String username) {
        log.info("Creating order for user: {}", username);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .username(username)
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (var itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productSku(itemReq.getProductSku())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .order(order)
                    .build();
            orderItems.add(item);
            totalAmount = totalAmount.add(itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> reservedItems = new ArrayList<>();
        try {
            for (OrderItem item : savedOrder.getItems()) {
                log.info("Requesting stock reservation for SKU: {}, Quantity: {}", item.getProductSku(), item.getQuantity());
                inventoryServiceClient.reserveStock(new ReservationRequest(item.getProductSku(), item.getQuantity()));
                reservedItems.add(item);
            }
        } catch (Exception e) {
            log.error("Stock reservation failed. Initiating Saga compensating rollback for successfully reserved items...", e);
            for (OrderItem reserved : reservedItems) {
                try {
                    inventoryServiceClient.releaseStock(new ReservationRequest(reserved.getProductSku(), reserved.getQuantity()));
                } catch (Exception re) {
                    log.error("Compensating action failed to release stock for SKU: {}", reserved.getProductSku(), re);
                }
            }
            throw new BusinessException("Checkout failed: stock reservation failed. " + e.getMessage(), ErrorCode.BUSINESS_ERROR);
        }

        // Publish event to Kafka
        OrderEvent orderEvent = mapToEvent(savedOrder, "ORDER_CREATED");
        orderEventPublisher.publishOrderEvent(orderEvent);

        log.info("Successfully created order: {}", savedOrder.getOrderNumber());
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber, String username) {
        log.info("Fetching order: {} for user: {}", orderNumber, username);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderNumber, ErrorCode.NOT_FOUND));

        if (!order.getUsername().equalsIgnoreCase(username)) {
            throw new BusinessException("Access denied to order detail", ErrorCode.FORBIDDEN);
        }
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUsername(String username) {
        log.info("Fetching all orders for user: {}", username);
        return orderRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public void processPaymentResult(String orderNumber, String paymentStatus) {
        log.info("Processing payment result for order: {}, Status: {}", orderNumber, paymentStatus);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderNumber, ErrorCode.NOT_FOUND));

        if (order.getStatus() != OrderStatus.CREATED) {
            log.warn("Order {} is already in status {}. Skipping payment processing.", orderNumber, order.getStatus());
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Order {} marked as PAID", orderNumber);

            OrderEvent orderEvent = mapToEvent(order, "ORDER_PAID");
            orderEventPublisher.publishOrderEvent(orderEvent);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order {} marked as CANCELLED due to payment failure", orderNumber);

            // Compensating action: Release reserved stock
            for (OrderItem item : order.getItems()) {
                try {
                    log.info("Releasing reserved stock for cancelled order {}, SKU: {}, Quantity: {}",
                            orderNumber, item.getProductSku(), item.getQuantity());
                    inventoryServiceClient.releaseStock(new ReservationRequest(item.getProductSku(), item.getQuantity()));
                } catch (Exception e) {
                    log.error("Failed to release stock for SKU: {} after payment failure", item.getProductSku(), e);
                }
            }

            OrderEvent orderEvent = mapToEvent(order, "ORDER_CANCELLED");
            orderEventPublisher.publishOrderEvent(orderEvent);
        }
    }

    private OrderEvent mapToEvent(Order order, String eventType) {
        List<OrderItemEvent> itemEvents = order.getItems().stream()
                .map(item -> OrderItemEvent.builder()
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        return OrderEvent.builder()
                .orderNumber(order.getOrderNumber())
                .username(order.getUsername())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .eventType(eventType)
                .items(itemEvents)
                .build();
    }
}
