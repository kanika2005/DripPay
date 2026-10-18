package com.drippay.service;

import com.drippay.domain.entity.*;
import com.drippay.domain.enums.LedgerEntryType;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import com.drippay.dto.transaction.AmountRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.dto.transaction.TransferRequest;
import com.drippay.exception.ApiException;
import com.drippay.kyc.KycLimitService;
import com.drippay.merchant.WebhookService;
import com.drippay.outbox.OutboxService;
import com.drippay.ratelimit.RateLimitService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final OutboxService outboxService;
    private final WebhookService webhookService;
    private final KycLimitService kycLimitService;
    private final RateLimitService rateLimitService;

    public TransferService(WalletRepository walletRepository,
                           WalletTransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           IdempotencyRecordRepository idempotencyRecordRepository,
                           TransactionCategoryRepository categoryRepository,
                           FraudWatchService fraudWatchService,
                           AuditService auditService,
                           OutboxService outboxService,
                           WebhookService webhookService,
                           KycLimitService kycLimitService,
                           RateLimitService rateLimitService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.categoryRepository = categoryRepository;
        this.fraudWatchService = fraudWatchService;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.webhookService = webhookService;
        this.kycLimitService = kycLimitService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public TransactionResponse safeTransfer(Long senderUserId, TransferRequest request, String idempotencyKey, String ip) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        rateLimitService.checkTransfer(senderUserId);
        BigDecimal amount = MoneyUtils.normalize(request.amount());
        MoneyUtils.requirePositive(amount);

        String requestHash = RequestHashUtil.sha256(
                request.recipientWalletId() + "|" + amount + "|" + nullToEmpty(request.note()));

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

            kycLimitService.assertTransferAllowed(senderUserId, sender, amount);

            WalletTransaction tx = new WalletTransaction();
            tx.setReferenceId(UUID.randomUUID().toString());
            tx.setType(TransactionType.P2P_TRANSFER);
            tx.setStatus(TransactionStatus.INITIATED);
            tx.setAmount(amount);
            tx.setSenderWalletId(sender.getId());
            tx.setRecipientWalletId(recipient.getId());
            tx.setInitiatedByUserId(senderUserId);
            tx.setCategoryId(categoryId);
            tx.setDescription(request.note());
            tx.setIdempotencyKey(idempotencyKey);
            tx = transactionRepository.save(tx);

            tx.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(tx);

            sender.setBalance(sender.getBalance().subtract(amount));
            recipient.setBalance(recipient.getBalance().add(amount));

            postLedger(tx.getId(), sender.getId(), LedgerEntryType.DEBIT, amount);
            postLedger(tx.getId(), recipient.getId(), LedgerEntryType.CREDIT, amount);

            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);

            IdempotencyRecord record = idempotencyRecordRepository
                    .findByUserIdAndIdempotencyKey(senderUserId, idempotencyKey)
                    .orElseGet(IdempotencyRecord::new);
            record.setUserId(senderUserId);
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(requestHash);
            record.setTransactionId(tx.getId());
            idempotencyRecordRepository.save(record);

            fraudWatchService.onSuccessfulTransfer(senderUserId, amount);
            auditService.log(senderUserId, "TRANSFER_COMPLETED", "ref=" + tx.getReferenceId(), ip);

            publishTransferEvents(tx, sender, recipient);

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
        tx.setStatus(TransactionStatus.INITIATED);
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
        tx.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(tx);

        if (creditUser) {
            postLedger(tx.getId(), platform.getId(), LedgerEntryType.DEBIT, amount);
            postLedger(tx.getId(), user.getId(), LedgerEntryType.CREDIT, amount);
        } else {
            postLedger(tx.getId(), user.getId(), LedgerEntryType.DEBIT, amount);
            postLedger(tx.getId(), platform.getId(), LedgerEntryType.CREDIT, amount);
        }

        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);

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

    @Transactional
    public TransactionResponse reverseTransfer(Long userId, Long originalTxnId, String ip) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        WalletTransaction original = transactionRepository.findById(originalTxnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!wallet.getId().equals(original.getSenderWalletId()) && !wallet.getId().equals(original.getRecipientWalletId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to reverse this transaction");
        }
        if (original.getStatus() != TransactionStatus.COMPLETED && original.getStatus() != TransactionStatus.SUCCESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only completed transfers can be reversed");
        }
        if (original.getType() != TransactionType.P2P_TRANSFER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only P2P transfers can be reversed");
        }
        if (transactionRepository.existsByParentTxnId(originalTxnId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Transaction already reversed");
        }

        BigDecimal amount = original.getAmount();
        Long senderId = original.getSenderWalletId();
        Long recipientId = original.getRecipientWalletId();

        lockWalletsInOrder(senderId, recipientId);
        Wallet sender = walletRepository.findByIdForUpdate(senderId).orElseThrow();
        Wallet recipient = walletRepository.findByIdForUpdate(recipientId).orElseThrow();

        if (recipient.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recipient has insufficient balance to reverse");
        }

        recipient.setBalance(recipient.getBalance().subtract(amount));
        sender.setBalance(sender.getBalance().add(amount));

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        WalletTransaction reversal = new WalletTransaction();
        reversal.setReferenceId(UUID.randomUUID().toString());
        reversal.setType(TransactionType.P2P_TRANSFER);
        reversal.setStatus(TransactionStatus.SUCCESS);
        reversal.setAmount(amount);
        reversal.setSenderWalletId(recipientId);
        reversal.setRecipientWalletId(senderId);
        reversal.setInitiatedByUserId(userId);
        reversal.setParentTxnId(originalTxnId);
        reversal.setDescription("Reversal of " + original.getReferenceId());
        reversal = transactionRepository.save(reversal);

        postLedger(reversal.getId(), recipientId, LedgerEntryType.DEBIT, amount);
        postLedger(reversal.getId(), senderId, LedgerEntryType.CREDIT, amount);

        auditService.log(userId, "TRANSFER_REVERSED", "original=" + original.getReferenceId(), ip);
        return toResponse(reversal);
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

    private void publishTransferEvents(WalletTransaction tx, Wallet sender, Wallet recipient) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("referenceId", tx.getReferenceId());
        payload.put("amount", tx.getAmount());
        payload.put("currency", tx.getCurrency());
        payload.put("senderWalletId", sender.getPublicWalletId());
        payload.put("recipientWalletId", recipient.getPublicWalletId());
        outboxService.enqueue("PAYMENT_COMPLETED", payload);

        webhookService.enqueueForUser(recipient.getUserId(), "PAYMENT_RECEIVED", payload);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
