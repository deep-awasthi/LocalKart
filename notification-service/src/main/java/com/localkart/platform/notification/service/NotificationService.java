package com.localkart.platform.notification.service;

import com.localkart.platform.notification.messaging.event.OrderEvent;
import com.localkart.platform.notification.messaging.event.PaymentEvent;

public interface NotificationService {
    void processOrderNotification(OrderEvent orderEvent);
    void processPaymentNotification(PaymentEvent paymentEvent);
}
