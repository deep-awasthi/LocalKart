package com.localkart.platform.payment.service;

import com.localkart.platform.payment.domain.Payment;
import com.localkart.platform.payment.domain.PaymentStatus;
import com.localkart.platform.payment.repository.PaymentRepository;
import com.localkart.platform.payment.service.impl.PaymentServiceImpl;
import com.localkart.platform.payment.messaging.event.OrderEvent;
import com.localkart.platform.payment.messaging.event.PaymentEvent;
import com.localkart.platform.payment.messaging.publisher.PaymentEventPublisher;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void testProcessOrderPaymentSuccess() {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderNumber("ORD-123")
                .username("john")
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        Payment payment = Payment.builder()
                .transactionId("TRX-123")
                .orderNumber("ORD-123")
                .username("john")
                .amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        doNothing().when(paymentEventPublisher).publishPaymentEvent(any(PaymentEvent.class));

        paymentService.processOrderPayment(orderEvent);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventPublisher, times(1)).publishPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void testProcessOrderPaymentFailure() {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderNumber("ORD-123")
                .username("fail_user")
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        Payment payment = Payment.builder()
                .transactionId("TRX-123")
                .orderNumber("ORD-123")
                .username("fail_user")
                .amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.FAILED)
                .build();

        when(paymentRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        doNothing().when(paymentEventPublisher).publishPaymentEvent(any(PaymentEvent.class));

        paymentService.processOrderPayment(orderEvent);

        verify(paymentRepository, times(1)).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
        verify(paymentEventPublisher, times(1)).publishPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void testGetPaymentByOrderSuccess() {
        Payment payment = Payment.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(PaymentStatus.SUCCESS)
                .amount(BigDecimal.valueOf(10.00))
                .build();

        when(paymentRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByOrder("ORD-123", "john");

        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderNumber());
    }

    @Test
    void testGetPaymentByOrderAccessDenied() {
        Payment payment = Payment.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(PaymentStatus.SUCCESS)
                .amount(BigDecimal.valueOf(10.00))
                .build();

        when(paymentRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(payment));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.getPaymentByOrder("ORD-123", "jane"));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void testGetPaymentByOrderNotFound() {
        when(paymentRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.getPaymentByOrder("ORD-999", "john"));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }
}
