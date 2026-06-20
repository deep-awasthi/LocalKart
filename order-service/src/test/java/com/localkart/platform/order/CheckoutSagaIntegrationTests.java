package com.localkart.platform.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.order.client.InventoryServiceClient;
import com.localkart.platform.order.domain.Order;
import com.localkart.platform.order.domain.OrderStatus;
import com.localkart.platform.order.messaging.consumer.PaymentEventConsumer;
import com.localkart.platform.order.messaging.event.PaymentEvent;
import com.localkart.platform.order.messaging.publisher.OrderEventPublisher;
import com.localkart.platform.order.repository.OrderRepository;
import com.localkart.platform.order.web.dto.OrderItemRequest;
import com.localkart.platform.order.web.dto.OrderRequest;
import com.localkart.platform.order.web.dto.ReservationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutSagaIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentEventConsumer paymentEventConsumer;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @Test
    void testCheckoutSagaSuccess() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .items(Collections.singletonList(
                        new OrderItemRequest("SKU-SAGA-1", 3, BigDecimal.valueOf(15.00))
                ))
                .build();

        // Stub Feign stock reservation
        when(inventoryServiceClient.reserveStock(any(ReservationRequest.class))).thenReturn("Reserved");

        // 1. Submit Checkout REST Request
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String orderNumber = objectMapper.readTree(responseContent).path("data").path("orderNumber").asText();
        assertNotNull(orderNumber);

        // Verify Order is saved in H2 Database
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        assertNotNull(order);
        assertEquals(OrderStatus.CREATED, order.getStatus());
        verify(inventoryServiceClient, times(1)).reserveStock(eq(new ReservationRequest("SKU-SAGA-1", 3)));

        // 2. Simulate Payment success event arriving via Kafka
        PaymentEvent paymentSuccessEvent = PaymentEvent.builder()
                .orderNumber(orderNumber)
                .paymentStatus("SUCCESS")
                .transactionId("TRX-SAGA-SUCCESS")
                .amount(BigDecimal.valueOf(45.00))
                .build();

        paymentEventConsumer.consumePaymentEvent(paymentSuccessEvent);

        // Verify Order status transitioned to PAID in H2 Database
        Order updatedOrder = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());

        // Verify Order paid event published to Kafka
        verify(orderEventPublisher, times(2)).publishOrderEvent(any()); // 1 for CREATED, 1 for PAID
    }

    @Test
    void testCheckoutSagaPaymentFailureCompensating() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .items(Collections.singletonList(
                        new OrderItemRequest("SKU-SAGA-2", 2, BigDecimal.valueOf(20.00))
                ))
                .build();

        when(inventoryServiceClient.reserveStock(any(ReservationRequest.class))).thenReturn("Reserved");
        when(inventoryServiceClient.releaseStock(any(ReservationRequest.class))).thenReturn("Released");

        // 1. Submit Checkout REST Request
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String orderNumber = objectMapper.readTree(responseContent).path("data").path("orderNumber").asText();
        assertNotNull(orderNumber);

        // 2. Simulate Payment failure event arriving via Kafka
        PaymentEvent paymentFailureEvent = PaymentEvent.builder()
                .orderNumber(orderNumber)
                .paymentStatus("FAILED")
                .transactionId("TRX-SAGA-FAILED")
                .amount(BigDecimal.valueOf(40.00))
                .build();

        paymentEventConsumer.consumePaymentEvent(paymentFailureEvent);

        // Verify Order status transitioned to CANCELLED in H2 Database
        Order updatedOrder = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());

        // Verify Compensating action: stock release Feign client was invoked
        verify(inventoryServiceClient, times(1)).releaseStock(eq(new ReservationRequest("SKU-SAGA-2", 2)));

        // Verify Cancelled event published to Kafka
        verify(orderEventPublisher, times(2)).publishOrderEvent(any()); // 1 for CREATED, 1 for CANCELLED
    }
}
