package com.bookkeeping.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposits")
@Data
public class Deposit {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", type = org.hibernate.id.IncrementGenerator.class)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 50)
    private String depositType;

    @Column(nullable = false)
    private LocalDate depositTime;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column
    private Integer term;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDate reconciliationDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
