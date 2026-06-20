package com.localkart.platform.product.service.impl;

import com.localkart.platform.product.domain.Product;
import com.localkart.platform.product.domain.Review;
import com.localkart.platform.product.repository.ProductRepository;
import com.localkart.platform.product.repository.ReviewRepository;
import com.localkart.platform.product.service.ProductService;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {
        log.info("Creating product with SKU: {}", product.getSku());
        if (productRepository.existsBySku(product.getSku())) {
            throw new BusinessException("Product SKU already exists", ErrorCode.CONFLICT);
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        log.info("Fetching product by ID: {} (Cache Miss)", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found", ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public Product getProductBySku(String sku) {
        log.info("Fetching product by SKU: {} (Cache Miss)", sku);
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new BusinessException("Product not found", ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll();
    }

    @Override
    @Transactional
    public Review createReview(Review review) {
        log.info("Adding review to product: {}", review.getProductId());
        // Verify product existence
        if (!productRepository.existsById(review.getProductId())) {
            throw new BusinessException("Cannot review: Product does not exist", ErrorCode.NOT_FOUND);
        }
        return reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByProductId(Long productId) {
        log.info("Fetching reviews for product ID: {}", productId);
        return reviewRepository.findByProductId(productId);
    }
}
