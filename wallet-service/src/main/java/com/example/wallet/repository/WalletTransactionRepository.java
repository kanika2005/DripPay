package com.example.wallet.repository;

import com.example.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for wallet_transaction table.
 *
 * findByWalletIdOrderByCreatedAtDesc:
 * Spring Data reads the method name and generates SQL roughly like:
 *   SELECT * FROM wallet_transaction
 *   WHERE wallet_id = ?
 *   ORDER BY created_at DESC
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * WalletId resolves to wallet.id (nested property of the @ManyToOne association).
     */
    List<WalletTransaction> findByWallet_IdOrderByCreatedAtDesc(Long walletId);
}
