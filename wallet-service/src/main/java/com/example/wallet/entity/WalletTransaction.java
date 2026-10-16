package com.example.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity for table: wallet_transaction
 *
 * Relationship (simple):
 *   Many transactions belong to ONE wallet.
 *   That is a @ManyToOne from WalletTransaction → Wallet.
 *
 * We intentionally do NOT put a @OneToMany list on Wallet in Phase 1.
 * Why? Loading a wallet would also load all transactions (easy to misuse).
 * For history, we query WalletTransactionRepository by walletId instead.
 */
@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @ManyToOne = many rows in this table point to one wallet row.
     * @JoinColumn = the foreign key column name in THIS table.
     * FetchType.LAZY = do not load the Wallet from DB until we call getWallet().
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WalletTransaction() {
    }

    public WalletTransaction(Wallet wallet, TransactionType type, BigDecimal amount) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
