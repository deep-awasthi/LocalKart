package com.localkart.platform.notification.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String orderNumber;
    private String username;
    private String status;
    private BigDecimal totalAmount;
    private String eventType;
    private List<OrderItemEvent> items;
}
