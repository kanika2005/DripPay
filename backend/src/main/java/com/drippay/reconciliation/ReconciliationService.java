package com.drippay.reconciliation;

import com.drippay.repository.LedgerEntryRepository;
import com.drippay.repository.WalletRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReconciliationService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    private volatile ReconciliationReport lastReport = new ReconciliationReport(Instant.now(), List.of(), true);

    public ReconciliationService(WalletRepository walletRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialReconciliation() {
        lastReport = runCheck();
    }

    @Scheduled(cron = "${drippay.reconciliation.cron:0 0 2 * * *}")
    @Transactional(readOnly = true)
    public void nightlyReconciliation() {
        lastReport = runCheck();
    }

    @Transactional(readOnly = true)
    public ReconciliationReport runCheck() {
        List<ReconciliationMismatch> mismatches = new ArrayList<>();
        walletRepository.findAll().forEach(wallet -> {
            BigDecimal netLedger = ledgerEntryRepository.netBalanceForWallet(wallet.getId());
            if (netLedger == null) {
                netLedger = BigDecimal.ZERO;
            }
            if (wallet.getBalance().compareTo(netLedger) != 0) {
                mismatches.add(new ReconciliationMismatch(
                        wallet.getId(),
                        wallet.getPublicWalletId(),
                        wallet.getBalance(),
                        netLedger));
            }
        });
        return new ReconciliationReport(Instant.now(), mismatches, mismatches.isEmpty());
    }

    public ReconciliationReport getLastReport() {
        return lastReport;
    }

    public record ReconciliationMismatch(Long walletId, String publicWalletId, BigDecimal walletBalance, BigDecimal ledgerNet) {
    }

    public record ReconciliationReport(Instant ranAt, List<ReconciliationMismatch> mismatches, boolean balanced) {
    }
}
