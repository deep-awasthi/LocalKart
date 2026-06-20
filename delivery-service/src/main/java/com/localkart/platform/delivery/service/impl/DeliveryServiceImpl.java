package com.localkart.platform.delivery.service.impl;

import com.localkart.platform.delivery.client.UserClient;
import com.localkart.platform.delivery.domain.Delivery;
import com.localkart.platform.delivery.domain.DeliveryStatus;
import com.localkart.platform.delivery.repository.DeliveryRepository;
import com.localkart.platform.delivery.service.DeliveryService;
import com.localkart.platform.delivery.web.dto.DeliveryCreateRequest;
import com.localkart.platform.delivery.web.dto.UserDto;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserClient userClient;

    @Override
    @Transactional
    public Delivery createDelivery(DeliveryCreateRequest request) {
        log.info("Creating delivery manually for order: {}", request.getOrderNumber());
        if (deliveryRepository.findByOrderNumber(request.getOrderNumber()).isPresent()) {
            throw new BusinessException("Delivery already exists for order: " + request.getOrderNumber(), ErrorCode.BAD_REQUEST);
        }

        Delivery delivery = Delivery.builder()
                .orderNumber(request.getOrderNumber())
                .username(request.getUsername())
                .status(DeliveryStatus.PENDING)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .courier(request.getCourier() != null ? request.getCourier() : "LocalKart Express")
                .deliveryAddress(request.getDeliveryAddress())
                .build();

        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public Delivery createDeliveryFromOrder(String orderNumber, String username) {
        log.info("Creating delivery automatically from Kafka event for order: {} (user: {})", orderNumber, username);

        // Fix: store Optional to avoid double database query
        Optional<Delivery> existing = deliveryRepository.findByOrderNumber(orderNumber);
        if (existing.isPresent()) {
            log.warn("Delivery tracking already active for order: {}", orderNumber);
            return existing.get();
        }

        // Fetch shipping address dynamically via user Feign Client
        String address = "Default Shipping Address";
        try {
            UserDto userDto = userClient.getProfileByEmail(username);
            if (userDto != null && userDto.getAddress() != null && !userDto.getAddress().isBlank()) {
                address = userDto.getAddress();
            }
        } catch (Exception e) {
            log.error("Failed to fetch profile address for user {} via Feign. Falling back to default address.", username, e);
        }

        Delivery delivery = Delivery.builder()
                .orderNumber(orderNumber)
                .username(username)
                .status(DeliveryStatus.PENDING)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .courier("LocalKart Express")
                .deliveryAddress(address)
                .build();

        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void cancelDeliveryForOrder(String orderNumber) {
        log.info("Cancelling delivery automatically from Kafka event for order: {}", orderNumber);
        deliveryRepository.findByOrderNumber(orderNumber).ifPresent(delivery -> {
            delivery.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(delivery);
            log.info("Delivery status cancelled for order: {}", orderNumber);
        });
    }

    @Override
    @Transactional
    public Delivery updateDeliveryStatus(Long id, String status) {
        log.info("Updating delivery ID {} status to {}", id, status);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Delivery not found with id: " + id, ErrorCode.NOT_FOUND));
        
        DeliveryStatus newStatus;
        try {
            newStatus = DeliveryStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid delivery status: " + status + ". Valid values: " + 
                    java.util.Arrays.toString(DeliveryStatus.values()), ErrorCode.BAD_REQUEST);
        }

        delivery.setStatus(newStatus);
        return deliveryRepository.save(delivery);
    }

    @Override
    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Delivery not found with id: " + id, ErrorCode.NOT_FOUND));
    }

    @Override
    public Delivery getDeliveryByOrderNumber(String orderNumber) {
        return deliveryRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException("Delivery not found for order number: " + orderNumber, ErrorCode.NOT_FOUND));
    }

    @Override
    public List<Delivery> getDeliveriesByUser(String username) {
        return deliveryRepository.findByUsername(username);
    }

    @Override
    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }
}
