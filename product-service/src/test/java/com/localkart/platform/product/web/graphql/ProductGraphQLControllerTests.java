package com.localkart.platform.product.web.graphql;

import com.localkart.platform.product.domain.Product;
import com.localkart.platform.product.domain.Review;
import com.localkart.platform.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@GraphQlTest(controllers = ProductGraphQLController.class)
@ActiveProfiles("test")
class ProductGraphQLControllerTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private ProductService productService;

    @Test
    void testGetProductByIdQuery() {
        Product mockProduct = Product.builder()
                .id(1L)
                .name("Camera")
                .description("Professional DSL Camera")
                .price(BigDecimal.valueOf(899.99))
                .sku("CAM-99")
                .category("Electronics")
                .build();

        when(productService.getProductById(1L)).thenReturn(mockProduct);

        String document = """
                query {
                    getProductById(id: "1") {
                        id
                        name
                        description
                        price
                        sku
                        category
                    }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("getProductById")
                .matchesJson("""
                        {
                            "id": "1",
                            "name": "Camera",
                            "description": "Professional DSL Camera",
                            "price": 899.99,
                            "sku": "CAM-99",
                            "category": "Electronics"
                        }
                        """);
    }

    @Test
    void testGetProductReviewsSchemaMapping() {
        Product mockProduct = Product.builder().id(1L).sku("CAM-99").build();
        Review mockReview = Review.builder()
                .id("rev-100")
                .productId(1L)
                .username("testuser")
                .rating(5)
                .comment("Excellent DSL camera!")
                .build();

        when(productService.getProductById(1L)).thenReturn(mockProduct);
        when(productService.getReviewsByProductId(1L)).thenReturn(List.of(mockReview));

        String document = """
                query {
                    getProductById(id: "1") {
                        id
                        reviews {
                            id
                            username
                            rating
                            comment
                        }
                    }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("getProductById.reviews[0].id").entity(String.class).isEqualTo("rev-100")
                .path("getProductById.reviews[0].username").entity(String.class).isEqualTo("testuser")
                .path("getProductById.reviews[0].rating").entity(Integer.class).isEqualTo(5)
                .path("getProductById.reviews[0].comment").entity(String.class).isEqualTo("Excellent DSL camera!");
    }
}
