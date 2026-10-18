package com.drippay.domain.entity;

import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 36)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "sender_wallet_id")
    private Long senderWalletId;

    @Column(name = "recipient_wallet_id")
    private Long recipientWalletId;

    @Column(name = "initiated_by_user_id", nullable = false)
    private Long initiatedByUserId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(length = 255)
    private String description;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "parent_txn_id")
    private Long parentTxnId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getSenderWalletId() { return senderWalletId; }
    public void setSenderWalletId(Long senderWalletId) { this.senderWalletId = senderWalletId; }
    public Long getRecipientWalletId() { return recipientWalletId; }
    public void setRecipientWalletId(Long recipientWalletId) { this.recipientWalletId = recipientWalletId; }
    public Long getInitiatedByUserId() { return initiatedByUserId; }
    public void setInitiatedByUserId(Long initiatedByUserId) { this.initiatedByUserId = initiatedByUserId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getParentTxnId() { return parentTxnId; }
    public void setParentTxnId(Long parentTxnId) { this.parentTxnId = parentTxnId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
