package com.localkart.platform.payment.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String orderNumber;
    private String paymentStatus; // e.g. "SUCCESS", "FAILED"
    private String transactionId;
    private BigDecimal amount;
}
