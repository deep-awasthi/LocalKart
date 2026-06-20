package com.localkart.platform.notification.messaging.consumer;

import com.localkart.platform.notification.messaging.event.OrderEvent;
import com.localkart.platform.notification.messaging.event.PaymentEvent;
import com.localkart.platform.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void consumeOrderEvent(OrderEvent event) {
        log.info("Notification Consumer received OrderEvent for order: {}, type: {}", event.getOrderNumber(), event.getEventType());
        try {
            notificationService.processOrderNotification(event);
        } catch (Exception e) {
            log.error("Error processing OrderEvent for order: {}", event.getOrderNumber(), e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("Notification Consumer received PaymentEvent for order: {}, status: {}", event.getOrderNumber(), event.getPaymentStatus());
        try {
            notificationService.processPaymentNotification(event);
        } catch (Exception e) {
            log.error("Error processing PaymentEvent for order: {}", event.getOrderNumber(), e);
        }
    }
}
