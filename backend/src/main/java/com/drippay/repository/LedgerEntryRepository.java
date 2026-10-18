package com.drippay.repository;

import com.drippay.domain.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionId(Long transactionId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN e.entryType = com.drippay.domain.enums.LedgerEntryType.CREDIT THEN e.amount
                WHEN e.entryType = com.drippay.domain.enums.LedgerEntryType.DEBIT THEN -e.amount ELSE 0 END), 0)
            FROM LedgerEntry e WHERE e.walletId = :walletId
            """)
    BigDecimal netBalanceForWallet(@Param("walletId") Long walletId);
}
