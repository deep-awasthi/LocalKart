package com.localkart.platform.payment.service.impl;

import com.localkart.platform.payment.domain.Payment;
import com.localkart.platform.payment.domain.PaymentStatus;
import com.localkart.platform.payment.repository.PaymentRepository;
import com.localkart.platform.payment.service.PaymentService;
import com.localkart.platform.payment.messaging.event.OrderEvent;
import com.localkart.platform.payment.messaging.event.PaymentEvent;
import com.localkart.platform.payment.messaging.publisher.PaymentEventPublisher;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public void processOrderPayment(OrderEvent orderEvent) {
        log.info("Processing payment for order: {}, total amount: {}", orderEvent.getOrderNumber(), orderEvent.getTotalAmount());

        if (paymentRepository.findByOrderNumber(orderEvent.getOrderNumber()).isPresent()) {
            log.warn("Payment for order {} has already been processed.", orderEvent.getOrderNumber());
            return;
        }

        // Simulation logic: Username containing "fail" initiates payment failure
        PaymentStatus status = PaymentStatus.SUCCESS;
        if (orderEvent.getUsername() != null && orderEvent.getUsername().toLowerCase().contains("fail")) {
            status = PaymentStatus.FAILED;
        }

        Payment payment = Payment.builder()
                .transactionId("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderNumber(orderEvent.getOrderNumber())
                .username(orderEvent.getUsername())
                .amount(orderEvent.getTotalAmount())
                .status(status)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved with status: {} and transaction ID: {}", savedPayment.getStatus(), savedPayment.getTransactionId());

        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderNumber(savedPayment.getOrderNumber())
                .paymentStatus(savedPayment.getStatus().name())
                .transactionId(savedPayment.getTransactionId())
                .amount(savedPayment.getAmount())
                .build();

        paymentEventPublisher.publishPaymentEvent(paymentEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrder(String orderNumber, String username) {
        log.info("Fetching payment details for order: {} by user: {}", orderNumber, username);
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException("Payment record not found for order: " + orderNumber, ErrorCode.NOT_FOUND));

        if (!payment.getUsername().equalsIgnoreCase(username)) {
            throw new BusinessException("Access denied to transaction details", ErrorCode.FORBIDDEN);
        }
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        log.info("Fetching all transaction logs");
        return paymentRepository.findAll();
    }
}
