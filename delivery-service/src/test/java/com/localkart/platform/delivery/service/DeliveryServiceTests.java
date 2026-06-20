package com.localkart.platform.delivery.service;

import com.localkart.platform.delivery.client.UserClient;
import com.localkart.platform.delivery.domain.Delivery;
import com.localkart.platform.delivery.domain.DeliveryStatus;
import com.localkart.platform.delivery.repository.DeliveryRepository;
import com.localkart.platform.delivery.service.impl.DeliveryServiceImpl;
import com.localkart.platform.delivery.web.dto.DeliveryCreateRequest;
import com.localkart.platform.delivery.web.dto.UserDto;
import com.localkart.platform.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTests {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private Delivery sampleDelivery;

    @BeforeEach
    void setUp() {
        sampleDelivery = Delivery.builder()
                .id(1L)
                .orderNumber("ORD-100")
                .username("john@example.com")
                .status(DeliveryStatus.PENDING)
                .trackingNumber("TRK-12345")
                .courier("LocalKart Express")
                .deliveryAddress("123 Test St")
                .build();
    }

    @Test
    void createDelivery_WhenDeliveryAlreadyExists_ShouldThrowException() {
        DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                .orderNumber("ORD-100")
                .username("john@example.com")
                .courier("Courier A")
                .deliveryAddress("Address A")
                .build();

        when(deliveryRepository.findByOrderNumber("ORD-100")).thenReturn(Optional.of(sampleDelivery));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Delivery already exists");

        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void createDelivery_WhenSuccess_ShouldSaveAndReturn() {
        DeliveryCreateRequest request = DeliveryCreateRequest.builder()
                .orderNumber("ORD-200")
                .username("john@example.com")
                .courier("Courier A")
                .deliveryAddress("Address A")
                .build();

        when(deliveryRepository.findByOrderNumber("ORD-200")).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.createDelivery(request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-200");
        assertThat(result.getCourier()).isEqualTo("Courier A");
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(result.getTrackingNumber()).startsWith("TRK-");
        
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void createDeliveryFromOrder_ShouldFetchUserAddressViaFeign() {
        String username = "john@example.com";
        String orderNumber = "ORD-300";
        UserDto userDto = UserDto.builder()
                .email(username)
                .name("John Doe")
                .phone("1234567")
                .address("456 User St, City")
                .build();

        when(deliveryRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());
        when(userClient.getProfileByEmail(username)).thenReturn(userDto);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.createDeliveryFromOrder(orderNumber, username);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(result.getDeliveryAddress()).isEqualTo("456 User St, City");
        
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void createDeliveryFromOrder_WhenFeignFails_ShouldFallbackToDefaultAddress() {
        String username = "john@example.com";
        String orderNumber = "ORD-300";

        when(deliveryRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());
        when(userClient.getProfileByEmail(username)).thenThrow(new RuntimeException("Service offline"));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.createDeliveryFromOrder(orderNumber, username);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(result.getDeliveryAddress()).isEqualTo("Default Shipping Address");

        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    void createDeliveryFromOrder_WhenAlreadyExists_ShouldReturnExisting() {
        String orderNumber = "ORD-100";
        String username = "john@example.com";

        when(deliveryRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(sampleDelivery));

        Delivery result = deliveryService.createDeliveryFromOrder(orderNumber, username);

        assertThat(result).isEqualTo(sampleDelivery);
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void cancelDeliveryForOrder_WhenDeliveryFound_ShouldUpdateStatusToCancelled() {
        String orderNumber = "ORD-100";
        when(deliveryRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(sampleDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deliveryService.cancelDeliveryForOrder(orderNumber);

        assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        verify(deliveryRepository, times(1)).save(sampleDelivery);
    }

    @Test
    void updateDeliveryStatus_WhenInvalidStatus_ShouldThrowException() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(sampleDelivery));

        assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(1L, "INVALID_STATE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid delivery status");
    }

    @Test
    void updateDeliveryStatus_WhenValidStatus_ShouldSaveAndReturn() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(sampleDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.updateDeliveryStatus(1L, "SHIPPED");

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }
}
