package com.localkart.platform.notification.service;

import com.localkart.platform.notification.messaging.event.OrderEvent;
import com.localkart.platform.notification.messaging.event.PaymentEvent;
import com.localkart.platform.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void testProcessOrderNotificationCreated() {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status("CREATED")
                .totalAmount(BigDecimal.valueOf(45.00))
                .eventType("ORDER_CREATED")
                .build();

        assertDoesNotThrow(() -> notificationService.processOrderNotification(orderEvent));
    }

    @Test
    void testProcessOrderNotificationPaid() {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status("PAID")
                .totalAmount(BigDecimal.valueOf(45.00))
                .eventType("ORDER_PAID")
                .build();

        assertDoesNotThrow(() -> notificationService.processOrderNotification(orderEvent));
    }

    @Test
    void testProcessOrderNotificationCancelled() {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status("CANCELLED")
                .totalAmount(BigDecimal.valueOf(45.00))
                .eventType("ORDER_CANCELLED")
                .build();

        assertDoesNotThrow(() -> notificationService.processOrderNotification(orderEvent));
    }

    @Test
    void testProcessPaymentNotification() {
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderNumber("ORD-123")
                .paymentStatus("SUCCESS")
                .transactionId("TRX-123")
                .amount(BigDecimal.valueOf(45.00))
                .build();

        assertDoesNotThrow(() -> notificationService.processPaymentNotification(paymentEvent));
    }
}
