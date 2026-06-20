package com.localkart.platform.order.web.rest;

import com.localkart.platform.order.domain.Order;
import com.localkart.platform.order.service.OrderService;
import com.localkart.platform.order.web.dto.OrderRequest;
import com.localkart.platform.order.web.dto.OrderItemResponse;
import com.localkart.platform.order.web.dto.OrderResponse;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.security.SecurityContextUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        String username = SecurityContextUtils.getUsername();
        log.info("REST request to create order for user: {}", username);
        if (username == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        Order created = orderService.createOrder(request, username);
        return mapToResponse(created);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse getOrderByNumber(@PathVariable("orderNumber") String orderNumber) {
        String username = SecurityContextUtils.getUsername();
        log.info("REST request to fetch order details: {} for user: {}", orderNumber, username);
        if (username == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        Order order = orderService.getOrderByNumber(orderNumber, username);
        return mapToResponse(order);
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        String username = SecurityContextUtils.getUsername();
        log.info("REST request to fetch all orders for user: {}", username);
        if (username == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        List<Order> orders = orderService.getOrdersByUsername(username);
        return orders.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .username(order.getUsername())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
