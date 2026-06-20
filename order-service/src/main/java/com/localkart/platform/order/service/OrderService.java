package com.localkart.platform.order.service;

import com.localkart.platform.order.domain.Order;
import com.localkart.platform.order.web.dto.OrderRequest;

import java.util.List;

public interface OrderService {
    Order createOrder(OrderRequest request, String username);
    Order getOrderByNumber(String orderNumber, String username);
    List<Order> getOrdersByUsername(String username);
    void processPaymentResult(String orderNumber, String paymentStatus);
}
