package com.localkart.platform.notification;

import com.localkart.platform.notification.messaging.consumer.NotificationConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @MockBean
    private NotificationConsumer notificationConsumer;

    @Test
    void contextLoads() {
    }
}
