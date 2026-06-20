package com.localkart.platform.inventory.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.inventory.config.SecurityConfig;
import com.localkart.platform.inventory.domain.Inventory;
import com.localkart.platform.inventory.service.InventoryService;
import com.localkart.platform.inventory.web.dto.ReservationRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseWrapperAdvice.class})
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void testAddInventorySuccess() throws Exception {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(50)
                .reservedQuantity(0)
                .build();

        when(inventoryService.addInventory(any(Inventory.class))).thenReturn(inventory);

        mockMvc.perform(post("/api/v1/inventory")
                        .with(csrf())
                        .header("X-Auth-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productSku").value("SKU-100"))
                .andExpect(jsonPath("$.data.quantity").value(50))
                .andExpect(jsonPath("$.data.availableQuantity").value(50));
    }

    @Test
    void testAddInventoryValidationFailure() throws Exception {
        Inventory invalidInventory = Inventory.builder()
                .productSku("") // Blank
                .quantity(-10) // Negative
                .reservedQuantity(0)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .with(csrf())
                        .header("X-Auth-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidInventory)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-400"));
    }

    @Test
    void testAddInventoryUnauthenticated() throws Exception {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(50)
                .reservedQuantity(0)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .with(csrf())
                        // No header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventory)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetStockBySkuSuccess() throws Exception {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(50)
                .reservedQuantity(10)
                .build();

        when(inventoryService.getInventoryBySku("SKU-100")).thenReturn(inventory);

        mockMvc.perform(get("/api/v1/inventory/SKU-100")
                        .header("X-Auth-User", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productSku").value("SKU-100"))
                .andExpect(jsonPath("$.data.quantity").value(50))
                .andExpect(jsonPath("$.data.reservedQuantity").value(10))
                .andExpect(jsonPath("$.data.availableQuantity").value(40));
    }

    @Test
    void testGetStockBySkuNotFound() throws Exception {
        when(inventoryService.getInventoryBySku("SKU-NOT-FOUND"))
                .thenThrow(new BusinessException("Inventory not found", ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/inventory/SKU-NOT-FOUND")
                        .header("X-Auth-User", "user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RES-404"));
    }

    @Test
    void testReserveStockSuccess() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .productSku("SKU-100")
                .quantity(5)
                .build();

        doNothing().when(inventoryService).reserveStock("SKU-100", 5);

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .with(csrf())
                        .header("X-Auth-User", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Stock reserved successfully"));

        verify(inventoryService, times(1)).reserveStock("SKU-100", 5);
    }

    @Test
    void testReserveStockValidationFailure() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .productSku("") // Blank
                .quantity(0) // Invalid
                .build();

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .with(csrf())
                        .header("X-Auth-User", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-400"));
    }

    @Test
    void testReleaseStockSuccess() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .productSku("SKU-100")
                .quantity(5)
                .build();

        doNothing().when(inventoryService).releaseStock("SKU-100", 5);

        mockMvc.perform(post("/api/v1/inventory/release")
                        .with(csrf())
                        .header("X-Auth-User", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Stock released successfully"));

        verify(inventoryService, times(1)).releaseStock("SKU-100", 5);
    }

    @Test
    void testDeductStockSuccess() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .productSku("SKU-100")
                .quantity(5)
                .build();

        doNothing().when(inventoryService).deductStock("SKU-100", 5);

        mockMvc.perform(post("/api/v1/inventory/deduct")
                        .with(csrf())
                        .header("X-Auth-User", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Stock deducted successfully"));

        verify(inventoryService, times(1)).deductStock("SKU-100", 5);
    }
}
