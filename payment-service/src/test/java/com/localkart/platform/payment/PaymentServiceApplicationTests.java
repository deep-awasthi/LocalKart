package com.localkart.platform.payment;

import com.localkart.platform.payment.messaging.consumer.OrderEventConsumer;
import com.localkart.platform.payment.messaging.publisher.PaymentEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceApplicationTests {

    @MockBean
    private OrderEventConsumer orderEventConsumer;

    @MockBean
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    void contextLoads() {
    }
}
