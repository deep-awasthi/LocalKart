package com.localkart.platform.inventory.service;

import com.localkart.platform.inventory.domain.Inventory;
import com.localkart.platform.inventory.repository.InventoryRepository;
import com.localkart.platform.inventory.service.impl.InventoryServiceImpl;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTests {

    @Mock
    private InventoryRepository repository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void testAddInventorySuccess() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(0)
                .build();

        when(repository.existsByProductSku("SKU-100")).thenReturn(false);
        when(repository.save(any(Inventory.class))).thenReturn(inventory);

        Inventory result = inventoryService.addInventory(inventory);

        assertNotNull(result);
        assertEquals("SKU-100", result.getProductSku());
        assertEquals(10, result.getQuantity());
        verify(repository, times(1)).save(inventory);
    }

    @Test
    void testAddInventoryAlreadyExists() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(0)
                .build();

        when(repository.existsByProductSku("SKU-100")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                inventoryService.addInventory(inventory));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(repository, never()).save(any(Inventory.class));
    }

    @Test
    void testGetInventoryBySkuSuccess() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(2)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));

        Inventory result = inventoryService.getInventoryBySku("SKU-100");

        assertNotNull(result);
        assertEquals("SKU-100", result.getProductSku());
        assertEquals(10, result.getQuantity());
        assertEquals(2, result.getReservedQuantity());
    }

    @Test
    void testGetInventoryBySkuNotFound() {
        when(repository.findByProductSku("SKU-NONEXIST")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                inventoryService.getInventoryBySku("SKU-NONEXIST"));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testReserveStockSuccess() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(2)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));
        when(repository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.reserveStock("SKU-100", 3);

        assertEquals(5, inventory.getReservedQuantity());
        verify(repository, times(1)).save(inventory);
    }

    @Test
    void testReserveStockInsufficient() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(8)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                inventoryService.reserveStock("SKU-100", 3));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        verify(repository, never()).save(any(Inventory.class));
    }

    @Test
    void testReleaseStockSuccess() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(5)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));
        when(repository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.releaseStock("SKU-100", 3);

        assertEquals(2, inventory.getReservedQuantity());
        verify(repository, times(1)).save(inventory);
    }

    @Test
    void testReleaseStockMoreThanReserved() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(2)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                inventoryService.releaseStock("SKU-100", 3));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        verify(repository, never()).save(any(Inventory.class));
    }

    @Test
    void testDeductStockSuccess() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(5)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));
        when(repository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.deductStock("SKU-100", 4);

        assertEquals(6, inventory.getQuantity());
        assertEquals(1, inventory.getReservedQuantity());
        verify(repository, times(1)).save(inventory);
    }

    @Test
    void testDeductStockInsufficientReserved() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(10)
                .reservedQuantity(3)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                inventoryService.deductStock("SKU-100", 4));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        verify(repository, never()).save(any(Inventory.class));
    }

    @Test
    void testDeductStockInsufficientTotalQuantity() {
        Inventory inventory = Inventory.builder()
                .productSku("SKU-100")
                .quantity(3)
                .reservedQuantity(3)
                .build();

        when(repository.findByProductSku("SKU-100")).thenReturn(Optional.of(inventory));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                inventoryService.deductStock("SKU-100", 4));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        verify(repository, never()).save(any(Inventory.class));
    }
}
