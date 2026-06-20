package com.localkart.platform.product.web.graphql;

import com.localkart.platform.product.domain.Product;
import com.localkart.platform.product.domain.Review;
import com.localkart.platform.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ProductGraphQLController {

    private final ProductService productService;

    @QueryMapping
    public Product getProductById(@Argument Long id) {
        log.info("GraphQL Query: getProductById({})", id);
        return productService.getProductById(id);
    }

    @QueryMapping
    public Product getProductBySku(@Argument String sku) {
        log.info("GraphQL Query: getProductBySku({})", sku);
        return productService.getProductBySku(sku);
    }

    @QueryMapping
    public List<Product> getAllProducts() {
        log.info("GraphQL Query: getAllProducts()");
        return productService.getAllProducts();
    }

    @QueryMapping
    public List<Review> getReviewsByProductId(@Argument Long productId) {
        log.info("GraphQL Query: getReviewsByProductId({})", productId);
        return productService.getReviewsByProductId(productId);
    }

    @SchemaMapping(typeName = "Product", field = "reviews")
    public List<Review> getReviews(Product product) {
        log.info("GraphQL Schema Mapping: Fetching reviews for product: {}", product.getId());
        return productService.getReviewsByProductId(product.getId());
    }

    @MutationMapping
    public Product createProduct(@Argument CreateProductInput input) {
        log.info("GraphQL Mutation: createProduct({})", input);
        Product product = Product.builder()
                .name(input.name())
                .description(input.description())
                .price(BigDecimal.valueOf(input.price()))
                .sku(input.sku())
                .category(input.category())
                .build();
        return productService.createProduct(product);
    }

    @MutationMapping
    public Review createReview(@Argument CreateReviewInput input) {
        log.info("GraphQL Mutation: createReview({})", input);
        Review review = Review.builder()
                .productId(input.productId())
                .username(input.username())
                .rating(input.rating())
                .comment(input.comment())
                .build();
        return productService.createReview(review);
    }

    // Java 21 Records for clean input parameters representation
    public record CreateProductInput(String name, String description, double price, String sku, String category) {}
    public record CreateReviewInput(Long productId, String username, Integer rating, String comment) {}
}
