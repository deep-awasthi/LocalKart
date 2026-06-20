package com.localkart.platform.inventory.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "inventories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product SKU must not be blank")
    @Column(nullable = false, unique = true)
    private String productSku;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 0, message = "Quantity must not be negative")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Reserved quantity must not be null")
    @Min(value = 0, message = "Reserved quantity must not be negative")
    @Column(nullable = false)
    private Integer reservedQuantity;
}
