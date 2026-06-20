package com.localkart.platform.delivery.messaging.consumer;

import com.localkart.platform.delivery.messaging.event.OrderEvent;
import com.localkart.platform.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidEventConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(topics = "order-events", groupId = "delivery-group")
    public void consumeOrderEvent(OrderEvent event) {
        log.info("Received OrderEvent: Order={}, Type={}, Status={}", event.getOrderNumber(), event.getEventType(), event.getStatus());
        
        if ("ORDER_PAID".equalsIgnoreCase(event.getEventType()) || "PAID".equalsIgnoreCase(event.getStatus())) {
            log.info("Order {} has been PAID. Initializing delivery tracking...", event.getOrderNumber());
            try {
                deliveryService.createDeliveryFromOrder(event.getOrderNumber(), event.getUsername());
            } catch (Exception e) {
                log.error("Failed to automatically initialize delivery tracking for Order {}", event.getOrderNumber(), e);
            }
        } else if ("ORDER_CANCELLED".equalsIgnoreCase(event.getEventType()) || "CANCELLED".equalsIgnoreCase(event.getStatus())) {
            log.info("Order {} has been CANCELLED. Cancelling delivery tracking...", event.getOrderNumber());
            try {
                deliveryService.cancelDeliveryForOrder(event.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to automatically cancel delivery tracking for Order {}", event.getOrderNumber(), e);
            }
        }
    }
}
