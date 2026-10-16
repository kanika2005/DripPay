package com.drippay.service;

import com.drippay.domain.entity.*;
import com.drippay.domain.enums.LedgerEntryType;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import com.drippay.dto.transaction.AmountRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.dto.transaction.TransferRequest;
import com.drippay.exception.ApiException;
import com.drippay.repository.*;
import com.drippay.util.MoneyUtils;
import com.drippay.util.RequestHashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SafeTransfer engine: idempotent P2P transfers with pessimistic wallet locks and double-entry ledger.
 */
@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final FraudWatchService fraudWatchService;
    private final AuditService auditService;

    public TransferService(WalletRepository walletRepository,
                           WalletTransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           IdempotencyRecordRepository idempotencyRecordRepository,
                           TransactionCategoryRepository categoryRepository,
                           FraudWatchService fraudWatchService,
                           AuditService auditService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.categoryRepository = categoryRepository;
        this.fraudWatchService = fraudWatchService;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionResponse safeTransfer(Long senderUserId, TransferRequest request, String idempotencyKey, String ip) {
        BigDecimal amount = MoneyUtils.normalize(request.amount());
        MoneyUtils.requirePositive(amount);

        String requestHash = RequestHashUtil.sha256(
                request.recipientWalletId() + "|" + amount + "|" + nullToEmpty(request.note()));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRecordRepository.findByUserIdAndIdempotencyKey(senderUserId, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Idempotency key reused with different parameters");
                }
                if (record.getTransactionId() != null) {
                    return transactionRepository.findById(record.getTransactionId())
                            .map(tx -> toResponse(tx))
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
                }
            }
        }

        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sender wallet not found"));

        Wallet recipientWallet = walletRepository.findByPublicWalletId(request.recipientWalletId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient wallet not found"));

        if (senderWallet.getId().equals(recipientWallet.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot transfer to your own wallet");
        }

        Long categoryId = resolveCategoryId(request.categoryCode());

        try {
            lockWalletsInOrder(senderWallet.getId(), recipientWallet.getId());

            Wallet sender = walletRepository.findByIdForUpdate(senderWallet.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sender wallet not found"));
            Wallet recipient = walletRepository.findByIdForUpdate(recipientWallet.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient wallet not found"));

            if (sender.getBalance().compareTo(amount) < 0) {
                fraudWatchService.onFailedTransfer(senderUserId);
                auditService.log(senderUserId, "TRANSFER_FAILED", "Insufficient funds", ip);
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance");
            }

            sender.setBalance(sender.getBalance().subtract(amount));
            recipient.setBalance(recipient.getBalance().add(amount));

            WalletTransaction tx = new WalletTransaction();
            tx.setReferenceId(UUID.randomUUID().toString());
            tx.setType(TransactionType.P2P_TRANSFER);
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setAmount(amount);
            tx.setSenderWalletId(sender.getId());
            tx.setRecipientWalletId(recipient.getId());
            tx.setInitiatedByUserId(senderUserId);
            tx.setCategoryId(categoryId);
            tx.setDescription(request.note());
            tx.setIdempotencyKey(idempotencyKey);
            tx = transactionRepository.save(tx);

            postLedger(tx.getId(), sender.getId(), LedgerEntryType.DEBIT, amount);
            postLedger(tx.getId(), recipient.getId(), LedgerEntryType.CREDIT, amount);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                IdempotencyRecord record = idempotencyRecordRepository
                        .findByUserIdAndIdempotencyKey(senderUserId, idempotencyKey)
                        .orElseGet(IdempotencyRecord::new);
                record.setUserId(senderUserId);
                record.setIdempotencyKey(idempotencyKey);
                record.setRequestHash(requestHash);
                record.setTransactionId(tx.getId());
                idempotencyRecordRepository.save(record);
            }

            fraudWatchService.onSuccessfulTransfer(senderUserId, amount);
            auditService.log(senderUserId, "TRANSFER_COMPLETED", "ref=" + tx.getReferenceId(), ip);

            return toResponse(tx);
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            auditService.log(senderUserId, "TRANSFER_ERROR", ex.getClass().getSimpleName(), ip);
            throw ex;
        }
    }

    @Transactional
    public TransactionResponse demoDeposit(Long userId, AmountRequest request, String ip) {
        return platformMovement(userId, request, TransactionType.DEMO_DEPOSIT, true, ip);
    }

    @Transactional
    public TransactionResponse demoWithdraw(Long userId, AmountRequest request, String ip) {
        return platformMovement(userId, request, TransactionType.DEMO_WITHDRAWAL, false, ip);
    }

    private TransactionResponse platformMovement(Long userId, AmountRequest request, TransactionType type,
                                                 boolean creditUser, String ip) {
        BigDecimal amount = MoneyUtils.normalize(request.amount());
        MoneyUtils.requirePositive(amount);

        Wallet userWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        lockWalletsInOrder(userWallet.getId(), Wallet.PLATFORM_WALLET_ID);

        Wallet user = walletRepository.findByIdForUpdate(userWallet.getId()).orElseThrow();
        Wallet platform = walletRepository.findByIdForUpdate(Wallet.PLATFORM_WALLET_ID).orElseThrow();

        if (!creditUser && user.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        if (creditUser) {
            user.setBalance(user.getBalance().add(amount));
            platform.setBalance(platform.getBalance().subtract(amount));
        } else {
            user.setBalance(user.getBalance().subtract(amount));
            platform.setBalance(platform.getBalance().add(amount));
        }

        Long transferCategoryId = categoryRepository.findByCode("TRANSFER").map(TransactionCategory::getId).orElse(null);

        WalletTransaction tx = new WalletTransaction();
        tx.setReferenceId(UUID.randomUUID().toString());
        tx.setType(type);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setAmount(amount);
        if (creditUser) {
            tx.setSenderWalletId(platform.getId());
            tx.setRecipientWalletId(user.getId());
        } else {
            tx.setSenderWalletId(user.getId());
            tx.setRecipientWalletId(platform.getId());
        }
        tx.setInitiatedByUserId(userId);
        tx.setCategoryId(transferCategoryId);
        tx.setDescription(request.note());
        tx = transactionRepository.save(tx);

        if (creditUser) {
            postLedger(tx.getId(), platform.getId(), LedgerEntryType.DEBIT, amount);
            postLedger(tx.getId(), user.getId(), LedgerEntryType.CREDIT, amount);
        } else {
            postLedger(tx.getId(), user.getId(), LedgerEntryType.DEBIT, amount);
            postLedger(tx.getId(), platform.getId(), LedgerEntryType.CREDIT, amount);
        }

        auditService.log(userId, type.name(), "ref=" + tx.getReferenceId(), ip);
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        WalletTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!isParticipant(wallet.getId(), tx)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view this transaction");
        }
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(Long userId, String referenceId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        WalletTransaction tx = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!isParticipant(wallet.getId(), tx)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view this transaction");
        }
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(Long userId, Long categoryId, Instant from, Instant to, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        return transactionRepository.searchForWallet(wallet.getId(), categoryId, from, to, pageable)
                .map(this::toResponse);
    }

    private void lockWalletsInOrder(Long a, Long b) {
        List<Long> ids = new ArrayList<>(List.of(a, b));
        ids.sort(Long::compareTo);
        for (Long id : ids) {
            walletRepository.findByIdForUpdate(id).orElseThrow(
                    () -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        }
    }

    private void postLedger(Long transactionId, Long walletId, LedgerEntryType type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionId(transactionId);
        entry.setWalletId(walletId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        ledgerEntryRepository.save(entry);
    }

    private Long resolveCategoryId(String code) {
        if (code == null || code.isBlank()) {
            return categoryRepository.findByCode("TRANSFER").map(TransactionCategory::getId).orElse(null);
        }
        return categoryRepository.findByCode(code.toUpperCase())
                .map(TransactionCategory::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown category"));
    }

    private boolean isParticipant(Long walletId, WalletTransaction tx) {
        return walletId.equals(tx.getSenderWalletId()) || walletId.equals(tx.getRecipientWalletId());
    }

    private TransactionResponse toResponse(WalletTransaction tx) {
        String senderPublic = tx.getSenderWalletId() == null ? null
                : walletRepository.findById(tx.getSenderWalletId()).map(Wallet::getPublicWalletId).orElse(null);
        String recipientPublic = tx.getRecipientWalletId() == null ? null
                : walletRepository.findById(tx.getRecipientWalletId()).map(Wallet::getPublicWalletId).orElse(null);
        String categoryCode = tx.getCategoryId() == null ? null
                : categoryRepository.findById(tx.getCategoryId()).map(TransactionCategory::getCode).orElse(null);

        return new TransactionResponse(
                tx.getId(),
                tx.getReferenceId(),
                tx.getType(),
                tx.getStatus(),
                tx.getAmount(),
                tx.getCurrency(),
                recipientPublic,
                senderPublic,
                tx.getDescription(),
                categoryCode,
                tx.getCreatedAt()
        );
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
