package com.localkart.platform.inventory.service;

import com.localkart.platform.inventory.domain.Inventory;

public interface InventoryService {
    Inventory addInventory(Inventory inventory);
    Inventory getInventoryBySku(String productSku);
    
    void reserveStock(String productSku, int quantity);
    void releaseStock(String productSku, int quantity);
    void deductStock(String productSku, int quantity);
}
