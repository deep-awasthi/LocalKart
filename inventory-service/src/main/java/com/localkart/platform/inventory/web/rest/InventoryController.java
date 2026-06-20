package com.localkart.platform.inventory.web.rest;

import com.localkart.platform.inventory.domain.Inventory;
import com.localkart.platform.inventory.service.InventoryService;
import com.localkart.platform.inventory.web.dto.InventoryResponse;
import com.localkart.platform.inventory.web.dto.ReservationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public InventoryResponse addInventory(@Valid @RequestBody Inventory inventory) {
        log.info("REST request to add inventory for SKU: {}", inventory.getProductSku());
        Inventory created = inventoryService.addInventory(inventory);
        return mapToResponse(created);
    }

    @GetMapping("/{sku}")
    public InventoryResponse getStockBySku(@PathVariable("sku") String sku) {
        log.info("REST request to get stock for SKU: {}", sku);
        Inventory inventory = inventoryService.getInventoryBySku(sku);
        return mapToResponse(inventory);
    }

    @PostMapping("/reserve")
    public String reserveStock(@Valid @RequestBody ReservationRequest request) {
        log.info("REST request to reserve stock: {}", request);
        inventoryService.reserveStock(request.getProductSku(), request.getQuantity());
        return "Stock reserved successfully";
    }

    @PostMapping("/release")
    public String releaseStock(@Valid @RequestBody ReservationRequest request) {
        log.info("REST request to release stock: {}", request);
        inventoryService.releaseStock(request.getProductSku(), request.getQuantity());
        return "Stock released successfully";
    }

    @PostMapping("/deduct")
    public String deductStock(@Valid @RequestBody ReservationRequest request) {
        log.info("REST request to deduct stock: {}", request);
        inventoryService.deductStock(request.getProductSku(), request.getQuantity());
        return "Stock deducted successfully";
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productSku(inventory.getProductSku())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getQuantity() - inventory.getReservedQuantity())
                .build();
    }
}
