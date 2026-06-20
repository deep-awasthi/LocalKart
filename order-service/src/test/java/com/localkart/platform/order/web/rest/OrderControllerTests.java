package com.localkart.platform.order.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.order.config.SecurityConfig;
import com.localkart.platform.order.domain.Order;
import com.localkart.platform.order.domain.OrderItem;
import com.localkart.platform.order.domain.OrderStatus;
import com.localkart.platform.order.service.OrderService;
import com.localkart.platform.order.web.dto.OrderItemRequest;
import com.localkart.platform.order.web.dto.OrderRequest;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseWrapperAdvice.class})
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void testCreateOrderSuccess() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .items(Collections.singletonList(
                        new OrderItemRequest("SKU-1", 5, BigDecimal.valueOf(10.00))
                ))
                .build();

        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(50.00))
                .items(new ArrayList<>())
                .build();
        order.addItem(OrderItem.builder().productSku("SKU-1").quantity(5).price(BigDecimal.valueOf(10.00)).build());

        when(orderService.createOrder(any(OrderRequest.class), eq("john"))).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.totalAmount").value(50.00))
                .andExpect(jsonPath("$.data.items[0].productSku").value("SKU-1"));
    }

    @Test
    void testCreateOrderValidationFailure() throws Exception {
        // Empty items list, and invalid price/qty on item request
        OrderRequest request = OrderRequest.builder()
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-400"));
    }

    @Test
    void testCreateOrderUnauthenticated() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .items(Collections.singletonList(
                        new OrderItemRequest("SKU-1", 5, BigDecimal.valueOf(10.00))
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        // No header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetOrderByNumberSuccess() throws Exception {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(50.00))
                .items(new ArrayList<>())
                .build();
        order.addItem(OrderItem.builder().productSku("SKU-1").quantity(5).price(BigDecimal.valueOf(10.00)).build());

        when(orderService.getOrderByNumber("ORD-123", "john")).thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/ORD-123")
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123"))
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void testGetOrderByNumberNotFound() throws Exception {
        when(orderService.getOrderByNumber("ORD-999", "john"))
                .thenThrow(new BusinessException("Order not found", ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/orders/ORD-999")
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RES-404"));
    }

    @Test
    void testGetOrdersSuccess() throws Exception {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .username("john")
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.valueOf(50.00))
                .items(new ArrayList<>())
                .build();

        when(orderService.getOrdersByUsername("john")).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Auth-User", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderNumber").value("ORD-123"));
    }
}
