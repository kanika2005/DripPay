package com.example.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity = Java class that maps to a database table.
 *
 * Table: wallet
 * Columns: id, user_id, balance, created_at, updated_at
 *
 * Why BigDecimal for balance (NOT double/float)?
 * ------------------------------------------------
 * Floating-point types (double) are binary approximations.
 * Example: 0.1 + 0.2 in double is NOT exactly 0.3.
 * Money must be exact. BigDecimal stores decimal values precisely
 * (with a defined scale), which is what finance systems need.
 *
 * What is @Entity?
 * - Tells JPA/Hibernate: "this class maps to a DB table"
 * - Without it, Spring Data JPA will not manage this class
 *
 * What is @Id + @GeneratedValue?
 * - @Id marks the primary key
 * - @GeneratedValue(IDENTITY) means MySQL AUTO_INCREMENT assigns the id
 */
@Entity
@Table(name = "wallet")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA requires a no-args constructor (Hibernate uses it to create instances).
     * We keep it package-visible / public for JPA.
     */
    public Wallet() {
    }

    public Wallet(Long userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    /**
     * @PrePersist runs right before INSERT.
     * Sets created_at / updated_at automatically so the service does not forget.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
    }

    /**
     * @PreUpdate runs right before UPDATE.
     */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
