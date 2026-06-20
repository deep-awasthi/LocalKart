package com.localkart.platform.delivery.web.rest;

import com.localkart.platform.delivery.domain.Delivery;
import com.localkart.platform.delivery.service.DeliveryService;
import com.localkart.platform.delivery.web.dto.DeliveryCreateRequest;
import com.localkart.platform.delivery.web.dto.DeliveryResponse;
import com.localkart.platform.delivery.web.dto.DeliveryStatusUpdateRequest;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.security.SecurityContextUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@Validated
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public DeliveryResponse createDelivery(@Valid @RequestBody DeliveryCreateRequest request) {
        log.info("REST request to create delivery: {}", request.getOrderNumber());
        Delivery created = deliveryService.createDelivery(request);
        return mapToResponse(created);
    }

    @GetMapping("/{id}")
    public DeliveryResponse getDeliveryById(@PathVariable("id") Long id) {
        log.info("REST request to get delivery by ID: {}", id);
        Delivery delivery = deliveryService.getDeliveryById(id);
        
        String currentUser = SecurityContextUtils.getUsername();
        Collection<String> roles = SecurityContextUtils.getRoles();
        if (currentUser != null && !roles.contains("ROLE_ADMIN") && !delivery.getUsername().equals(currentUser)) {
            throw new BusinessException("Access denied: You are not authorized to view this delivery.", ErrorCode.FORBIDDEN);
        }
        
        return mapToResponse(delivery);
    }

    @GetMapping("/order/{orderNumber}")
    public DeliveryResponse getDeliveryByOrderNumber(@PathVariable("orderNumber") String orderNumber) {
        log.info("REST request to get delivery by order number: {}", orderNumber);
        Delivery delivery = deliveryService.getDeliveryByOrderNumber(orderNumber);
        
        String currentUser = SecurityContextUtils.getUsername();
        Collection<String> roles = SecurityContextUtils.getRoles();
        if (currentUser != null && !roles.contains("ROLE_ADMIN") && !delivery.getUsername().equals(currentUser)) {
            throw new BusinessException("Access denied: You are not authorized to view this delivery.", ErrorCode.FORBIDDEN);
        }
        
        return mapToResponse(delivery);
    }

    @GetMapping
    public List<DeliveryResponse> getDeliveries() {
        String currentUser = SecurityContextUtils.getUsername();
        Collection<String> roles = SecurityContextUtils.getRoles();
        log.info("REST request to get deliveries for user: {}, roles: {}", currentUser, roles);
        
        List<Delivery> list;
        if (roles.contains("ROLE_ADMIN")) {
            list = deliveryService.getAllDeliveries();
        } else if (currentUser != null) {
            list = deliveryService.getDeliveriesByUser(currentUser);
        } else {
            throw new BusinessException("Access denied: Anonymous users cannot query deliveries.", ErrorCode.FORBIDDEN);
        }
        
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @PutMapping("/{id}/status")
    public DeliveryResponse updateDeliveryStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        log.info("REST request to update delivery ID {} status to {}", id, request.getStatus());
        Delivery updated = deliveryService.updateDeliveryStatus(id, request.getStatus());
        return mapToResponse(updated);
    }

    private DeliveryResponse mapToResponse(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .orderNumber(d.getOrderNumber())
                .username(d.getUsername())
                .status(d.getStatus().name())
                .trackingNumber(d.getTrackingNumber())
                .courier(d.getCourier())
                .deliveryAddress(d.getDeliveryAddress())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
