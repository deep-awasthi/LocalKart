package com.localkart.platform.delivery.repository;

import com.localkart.platform.delivery.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderNumber(String orderNumber);
    List<Delivery> findByUsername(String username);
}
