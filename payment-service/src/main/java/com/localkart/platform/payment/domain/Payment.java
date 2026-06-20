package com.localkart.platform.payment.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction ID must not be blank")
    @Column(nullable = false, unique = true)
    private String transactionId;

    @NotBlank(message = "Order number must not be blank")
    @Column(nullable = false)
    private String orderNumber;

    @NotBlank(message = "Username must not be blank")
    @Column(nullable = false)
    private String username;

    @NotNull(message = "Amount must not be null")
    @Min(value = 0, message = "Amount must not be negative")
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull(message = "Payment status must not be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
