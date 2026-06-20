package com.localkart.platform.order;

import com.localkart.platform.order.messaging.consumer.PaymentEventConsumer;
import com.localkart.platform.order.messaging.publisher.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @MockBean
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    void contextLoads() {
    }
}
