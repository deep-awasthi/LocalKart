package com.localkart.platform.product.service;

import com.localkart.platform.product.domain.Product;
import com.localkart.platform.product.domain.Review;

import java.util.List;

public interface ProductService {
    Product createProduct(Product product);
    Product getProductById(Long id);
    Product getProductBySku(String sku);
    List<Product> getAllProducts();
    
    Review createReview(Review review);
    List<Review> getReviewsByProductId(Long productId);
}
