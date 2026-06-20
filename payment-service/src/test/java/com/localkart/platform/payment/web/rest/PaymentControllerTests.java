package com.localkart.platform.payment.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.payment.config.SecurityConfig;
import com.localkart.platform.payment.domain.Payment;
import com.localkart.platform.payment.domain.PaymentStatus;
import com.localkart.platform.payment.service.PaymentService;
import com.localkart.platform.shared.advice.ApiResponseWrapperAdvice;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseWrapperAdvice.class})
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void testGetPaymentByOrderSuccess() throws Exception {
        Payment payment = Payment.builder()
                .transactionId("TRX-123")
                .orderNumber("ORD-123")
                .username("john")
                .amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentByOrder("ORD-123", "john")).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payments/ORD-123")
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("TRX-123"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.amount").value(100.00));
    }

    @Test
    void testGetPaymentByOrderUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/payments/ORD-123")
                        // No header
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetPaymentByOrderNotFound() throws Exception {
        when(paymentService.getPaymentByOrder("ORD-999", "john"))
                .thenThrow(new BusinessException("Payment record not found", ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/payments/ORD-999")
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RES-404"));
    }

    @Test
    void testGetAllPaymentsSuccess() throws Exception {
        Payment payment = Payment.builder()
                .transactionId("TRX-123")
                .orderNumber("ORD-123")
                .username("john")
                .amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getAllPayments()).thenReturn(Collections.singletonList(payment));

        mockMvc.perform(get("/api/v1/payments")
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].transactionId").value("TRX-123"));
    }
}
