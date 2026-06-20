package com.localkart.platform.payment.web.rest;

import com.localkart.platform.payment.domain.Payment;
import com.localkart.platform.payment.service.PaymentService;
import com.localkart.platform.payment.web.dto.PaymentResponse;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.security.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{orderNumber}")
    public PaymentResponse getPaymentByOrder(@PathVariable("orderNumber") String orderNumber) {
        String username = SecurityContextUtils.getUsername();
        log.info("REST request to fetch payment for order: {} by user: {}", orderNumber, username);
        if (username == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        Payment payment = paymentService.getPaymentByOrder(orderNumber, username);
        return mapToResponse(payment);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        String username = SecurityContextUtils.getUsername();
        log.info("REST request to fetch all payments by user: {}", username);
        if (username == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        // Admin or internally queries might query all, we map them directly
        List<Payment> payments = paymentService.getAllPayments();
        return payments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .transactionId(payment.getTransactionId())
                .orderNumber(payment.getOrderNumber())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
