package com.localkart.platform.product.service;

import com.localkart.platform.product.domain.Product;
import com.localkart.platform.product.domain.Review;
import com.localkart.platform.product.repository.ProductRepository;
import com.localkart.platform.product.repository.ReviewRepository;
import com.localkart.platform.product.service.impl.ProductServiceImpl;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void testCreateProductSuccess() {
        Product product = Product.builder()
                .sku("PROD-100")
                .name("Camera")
                .price(BigDecimal.valueOf(199.99))
                .category("Electronics")
                .build();

        when(productRepository.existsBySku(product.getSku())).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.createProduct(product);

        assertNotNull(result);
        assertEquals("PROD-100", result.getSku());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void testCreateProductSkuConflict() {
        Product product = Product.builder().sku("PROD-DUP").build();

        when(productRepository.existsBySku(product.getSku())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.createProduct(product));
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    void testGetProductByIdSuccess() {
        Product product = Product.builder().id(12L).sku("PROD-12").name("Phone").build();

        when(productRepository.findById(12L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(12L);

        assertNotNull(result);
        assertEquals("Phone", result.getName());
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.getProductById(99L));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testCreateReviewSuccess() {
        Review review = Review.builder()
                .productId(1L)
                .username("jane.doe")
                .rating(5)
                .comment("Excellent quality!")
                .build();

        when(productRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.save(review)).thenReturn(review);

        Review result = productService.createReview(review);

        assertNotNull(result);
        assertEquals("jane.doe", result.getUsername());
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void testCreateReviewProductNotFound() {
        Review review = Review.builder().productId(999L).build();

        when(productRepository.existsById(999L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> productService.createReview(review));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }
}
