package com.localkart.platform.order.client;

import com.localkart.platform.order.web.dto.ReservationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "${inventory-service.url:http://localhost:8084}")
public interface InventoryServiceClient {

    @PostMapping("/api/v1/inventory/reserve")
    String reserveStock(@RequestBody ReservationRequest request);

    @PostMapping("/api/v1/inventory/release")
    String releaseStock(@RequestBody ReservationRequest request);
}
