package com.localkart.platform.payment.web.dto;

import com.localkart.platform.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private String orderNumber;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
}
