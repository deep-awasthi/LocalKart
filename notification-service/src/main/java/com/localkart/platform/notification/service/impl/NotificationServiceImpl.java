package com.localkart.platform.notification.service.impl;

import com.localkart.platform.notification.messaging.event.OrderEvent;
import com.localkart.platform.notification.messaging.event.PaymentEvent;
import com.localkart.platform.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void processOrderNotification(OrderEvent orderEvent) {
        String eventType = orderEvent.getEventType();
        String orderNumber = orderEvent.getOrderNumber();
        String username = orderEvent.getUsername();

        log.info("Processing order notification for type: {}, order: {}", eventType, orderNumber);

        if ("ORDER_CREATED".equalsIgnoreCase(eventType)) {
            log.info("🔔 [NOTIFICATION SENT] Email/SMS template 'ORDER_PLACED' sent to user: '{}' for Order '{}'. Total Amount: ${}", 
                    username, orderNumber, orderEvent.getTotalAmount());
        } else if ("ORDER_PAID".equalsIgnoreCase(eventType)) {
            log.info("🔔 [NOTIFICATION SENT] Email/SMS template 'PAYMENT_RECEIVED' sent to user: '{}' for Order '{}'. Payment successful!", 
                    username, orderNumber);
        } else if ("ORDER_CANCELLED".equalsIgnoreCase(eventType)) {
            log.info("🔔 [NOTIFICATION SENT] Email/SMS template 'ORDER_CANCELLED' sent to user: '{}' for Order '{}'. Order has been voided.", 
                    username, orderNumber);
        } else {
            log.info("🔔 [NOTIFICATION SENT] Order update alert sent to user: '{}' for Order '{}'. New status: {}", 
                    username, orderNumber, orderEvent.getStatus());
        }
    }

    @Override
    public void processPaymentNotification(PaymentEvent paymentEvent) {
        log.info("Processing payment alert for order: {}, status: {}", 
                paymentEvent.getOrderNumber(), paymentEvent.getPaymentStatus());
        log.info("🔔 [PAYMENT RECORDED] Transaction details received for Order '{}'. Status: '{}', Transaction ID: '{}', Amount: ${}", 
                paymentEvent.getOrderNumber(), paymentEvent.getPaymentStatus(), paymentEvent.getTransactionId(), paymentEvent.getAmount());
    }
}
