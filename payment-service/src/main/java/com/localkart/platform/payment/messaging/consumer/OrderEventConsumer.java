package com.localkart.platform.payment.messaging.consumer;

import com.localkart.platform.payment.messaging.event.OrderEvent;
import com.localkart.platform.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void consumeOrderEvent(OrderEvent event) {
        log.info("Received OrderEvent for order: {}, type: {}", event.getOrderNumber(), event.getEventType());
        if ("ORDER_CREATED".equalsIgnoreCase(event.getEventType())) {
            try {
                paymentService.processOrderPayment(event);
            } catch (Exception e) {
                log.error("Error processing OrderEvent for order: {}", event.getOrderNumber(), e);
            }
        }
    }
}
