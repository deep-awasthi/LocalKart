package com.localkart.platform.order.messaging.consumer;

import com.localkart.platform.order.messaging.event.PaymentEvent;
import com.localkart.platform.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-group")
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent for order: {}, Status: {}", event.getOrderNumber(), event.getPaymentStatus());
        try {
            orderService.processPaymentResult(event.getOrderNumber(), event.getPaymentStatus());
        } catch (Exception e) {
            log.error("Error processing PaymentEvent for order: {}", event.getOrderNumber(), e);
        }
    }
}
