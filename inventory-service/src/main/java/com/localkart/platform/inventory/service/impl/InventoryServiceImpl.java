package com.localkart.platform.inventory.service.impl;

import com.localkart.platform.inventory.domain.Inventory;
import com.localkart.platform.inventory.repository.InventoryRepository;
import com.localkart.platform.inventory.service.InventoryService;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;

    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#inventory.productSku")
    public Inventory addInventory(Inventory inventory) {
        log.info("Adding inventory for SKU: {}", inventory.getProductSku());
        if (repository.existsByProductSku(inventory.getProductSku())) {
            throw new BusinessException("Inventory entry already exists for SKU: " + inventory.getProductSku(), ErrorCode.CONFLICT);
        }
        return repository.save(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "stock", key = "#productSku")
    public Inventory getInventoryBySku(String productSku) {
        log.info("Fetching stock level for SKU: {} (Cache Miss)", productSku);
        return repository.findByProductSku(productSku)
                .orElseThrow(() -> new BusinessException("Inventory not found for SKU: " + productSku, ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#productSku")
    public void reserveStock(String productSku, int quantity) {
        log.info("Attempting to reserve {} items for SKU: {}", quantity, productSku);
        Inventory inventory = getInventoryBySku(productSku);

        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            throw new BusinessException("Insufficient stock available for reservation", ErrorCode.BUSINESS_ERROR);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        repository.save(inventory);
        log.info("Successfully reserved {} items for SKU: {}", quantity, productSku);
    }

    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#productSku")
    public void releaseStock(String productSku, int quantity) {
        log.info("Attempting to release {} reserved items for SKU: {}", quantity, productSku);
        Inventory inventory = getInventoryBySku(productSku);

        if (inventory.getReservedQuantity() < quantity) {
            throw new BusinessException("Cannot release more than currently reserved stock", ErrorCode.BAD_REQUEST);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        repository.save(inventory);
        log.info("Successfully released {} reserved items for SKU: {}", quantity, productSku);
    }

    @Override
    @Transactional
    @CacheEvict(value = "stock", key = "#productSku")
    public void deductStock(String productSku, int quantity) {
        log.info("Attempting to deduct {} items from SKU: {}", quantity, productSku);
        Inventory inventory = getInventoryBySku(productSku);

        if (inventory.getReservedQuantity() < quantity || inventory.getQuantity() < quantity) {
            throw new BusinessException("Insufficient stock levels to complete deduction", ErrorCode.BUSINESS_ERROR);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        repository.save(inventory);
        log.info("Successfully deducted {} items for SKU: {}", quantity, productSku);
    }
}
