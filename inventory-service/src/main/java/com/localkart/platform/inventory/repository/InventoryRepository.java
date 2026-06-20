package com.localkart.platform.inventory.repository;

import com.localkart.platform.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductSku(String productSku);
    boolean existsByProductSku(String productSku);
}
