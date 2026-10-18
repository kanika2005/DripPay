package com.drippay.repository;

import com.drippay.domain.entity.WalletTransaction;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByReferenceId(String referenceId);

    @Query("""
        SELECT t FROM WalletTransaction t
        WHERE (t.senderWalletId = :walletId OR t.recipientWalletId = :walletId)
        AND (:categoryId IS NULL OR t.categoryId = :categoryId)
        AND (:from IS NULL OR t.createdAt >= :from)
        AND (:to IS NULL OR t.createdAt <= :to)
        AND t.status IN (com.drippay.domain.enums.TransactionStatus.COMPLETED,
            com.drippay.domain.enums.TransactionStatus.SUCCESS)
        ORDER BY t.createdAt DESC
        """)
    Page<WalletTransaction> searchForWallet(
            @Param("walletId") Long walletId,
            @Param("categoryId") Long categoryId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t
        WHERE t.senderWalletId = :walletId
        AND t.status = :status
        AND t.createdAt >= :from AND t.createdAt < :to
        """)
    BigDecimal sumSent(@Param("walletId") Long walletId, @Param("status") TransactionStatus status,
                       @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t
        WHERE t.senderWalletId = :walletId
        AND t.status IN (com.drippay.domain.enums.TransactionStatus.COMPLETED,
            com.drippay.domain.enums.TransactionStatus.SUCCESS)
        AND t.createdAt >= :from AND t.createdAt < :to
        """)
    BigDecimal sumSentSettled(@Param("walletId") Long walletId,
                              @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t
        WHERE t.recipientWalletId = :walletId
        AND t.status = :status
        AND t.createdAt >= :from AND t.createdAt < :to
        """)
    BigDecimal sumReceived(@Param("walletId") Long walletId, @Param("status") TransactionStatus status,
                           @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t
        WHERE t.recipientWalletId = :walletId
        AND t.status IN (com.drippay.domain.enums.TransactionStatus.COMPLETED,
            com.drippay.domain.enums.TransactionStatus.SUCCESS)
        AND t.createdAt >= :from AND t.createdAt < :to
        """)
    BigDecimal sumReceivedSettled(@Param("walletId") Long walletId,
                                  @Param("from") Instant from, @Param("to") Instant to);

    long countByInitiatedByUserIdAndStatusAndTypeAndCreatedAtAfter(
            Long userId, TransactionStatus status, TransactionType type, Instant after);

    long countByInitiatedByUserIdAndCreatedAtAfter(Long userId, Instant after);

    boolean existsByParentTxnId(Long parentTxnId);

    @Query("""
        SELECT t FROM WalletTransaction t
        WHERE t.senderWalletId = :walletId
        AND t.type = com.drippay.domain.enums.TransactionType.P2P_TRANSFER
        AND t.status IN (com.drippay.domain.enums.TransactionStatus.COMPLETED,
            com.drippay.domain.enums.TransactionStatus.SUCCESS)
        ORDER BY t.createdAt DESC
        """)
    List<WalletTransaction> findRecentOutboundP2P(@Param("walletId") Long walletId, Pageable pageable);
}
