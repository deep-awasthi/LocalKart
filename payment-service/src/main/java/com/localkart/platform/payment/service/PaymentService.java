package com.localkart.platform.payment.service;

import com.localkart.platform.payment.domain.Payment;
import com.localkart.platform.payment.messaging.event.OrderEvent;

import java.util.List;

public interface PaymentService {
    void processOrderPayment(OrderEvent orderEvent);
    Payment getPaymentByOrder(String orderNumber, String username);
    List<Payment> getAllPayments();
}
