package com.localkart.platform.order.messaging.publisher;

import com.localkart.platform.order.messaging.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "order-events";

    public void publishOrderEvent(OrderEvent event) {
        log.info("Publishing OrderEvent of type {} for order number: {}", event.getEventType(), event.getOrderNumber());
        try {
            kafkaTemplate.send(TOPIC, event.getOrderNumber(), event);
        } catch (Exception e) {
            log.error("Failed to publish OrderEvent for order number: {}", event.getOrderNumber(), e);
        }
    }
}
