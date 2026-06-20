package com.localkart.platform.delivery.service;

import com.localkart.platform.delivery.domain.Delivery;
import com.localkart.platform.delivery.web.dto.DeliveryCreateRequest;

import java.util.List;

public interface DeliveryService {
    Delivery createDelivery(DeliveryCreateRequest request);
    Delivery createDeliveryFromOrder(String orderNumber, String username);
    void cancelDeliveryForOrder(String orderNumber);
    Delivery updateDeliveryStatus(Long id, String status);
    Delivery getDeliveryById(Long id);
    Delivery getDeliveryByOrderNumber(String orderNumber);
    List<Delivery> getDeliveriesByUser(String username);
    List<Delivery> getAllDeliveries();
}
