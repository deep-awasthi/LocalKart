package com.localkart.platform.payment.messaging.publisher;

import com.localkart.platform.payment.messaging.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private static final String TOPIC = "payment-events";

    public void publishPaymentEvent(PaymentEvent event) {
        log.info("Publishing PaymentEvent for order: {}, Status: {}", event.getOrderNumber(), event.getPaymentStatus());
        try {
            kafkaTemplate.send(TOPIC, event.getOrderNumber(), event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentEvent for order: {}", event.getOrderNumber(), e);
        }
    }
}
