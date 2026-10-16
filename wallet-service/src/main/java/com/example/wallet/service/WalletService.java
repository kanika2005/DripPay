package com.example.wallet.service;

import com.example.wallet.dto.AddMoneyRequest;
import com.example.wallet.dto.CreateWalletRequest;
import com.example.wallet.dto.TransactionResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.dto.WithdrawMoneyRequest;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.InvalidWalletOperationException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service = business logic layer.
 *
 * What is @Service?
 * -----------------
 * Spring detects this class at startup (component scan),
 * creates ONE bean (object), and can inject it into WalletController.
 *
 * Flow without DI (bad):
 *   Controller does: new WalletService(new WalletRepository(...))  ← hard to test
 *
 * Flow with DI (good):
 *   Spring creates WalletRepository bean
 *        ↓
 *   Spring creates WalletService bean (injects repository via constructor)
 *        ↓
 *   Spring creates WalletController bean (injects service via constructor)
 *
 * Why separate Controller and Service?
 * - Controller: HTTP concerns (URL, status codes, JSON)
 * - Service: business rules (credit, debit, insufficient balance)
 * - Repository: database access
 * This keeps each layer focused and testable.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Constructor injection.
     * Spring sees WalletService needs these two repositories and passes the beans in.
     * Prefer constructor injection over @Autowired fields (clear, testable, immutable).
     */
    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    /**
     * Create a new wallet with balance = 0 for the given user.
     */
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        if (request.getUserId() == null) {
            throw new InvalidWalletOperationException("userId is required");
        }

        walletRepository.findByUserId(request.getUserId()).ifPresent(existing -> {
            throw new InvalidWalletOperationException(
                    "Wallet already exists for userId: " + request.getUserId());
        });

        Wallet wallet = new Wallet(request.getUserId(), BigDecimal.ZERO);
        Wallet saved = walletRepository.save(wallet);
        return toWalletResponse(saved);
    }

    /**
     * Get wallet details (also used as "check balance").
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long walletId) {
        Wallet wallet = findWalletOrThrow(walletId);
        return toWalletResponse(wallet);
    }

    /**
     * Credit: newBalance = oldBalance + amount
     */
    @Transactional
    public WalletResponse addMoney(Long walletId, AddMoneyRequest request) {
        BigDecimal amount = requirePositiveAmount(request.getAmount());

        Wallet wallet = findWalletOrThrow(walletId);
        wallet.setBalance(wallet.getBalance().add(amount));

        WalletTransaction transaction = new WalletTransaction(wallet, TransactionType.CREDIT, amount);
        walletTransactionRepository.save(transaction);

        // wallet is a managed JPA entity inside @Transactional,
        // so balance change is flushed to DB at commit.
        return toWalletResponse(wallet);
    }

    /**
     * Debit: newBalance = oldBalance - amount
     * Rule: amount cannot be greater than balance
     */
    @Transactional
    public WalletResponse withdrawMoney(Long walletId, WithdrawMoneyRequest request) {
        BigDecimal amount = requirePositiveAmount(request.getAmount());

        Wallet wallet = findWalletOrThrow(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InvalidWalletOperationException(
                    "Insufficient balance. Current balance: " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));

        WalletTransaction transaction = new WalletTransaction(wallet, TransactionType.DEBIT, amount);
        walletTransactionRepository.save(transaction);

        return toWalletResponse(wallet);
    }

    /**
     * Transaction history for a wallet (newest first).
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long walletId) {
        // Ensure wallet exists before listing transactions
        findWalletOrThrow(walletId);

        return walletTransactionRepository.findByWallet_IdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(tx -> toTransactionResponse(tx, walletId))
                .toList();
    }

    private Wallet findWalletOrThrow(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWalletOperationException("amount must be greater than 0");
        }
        return amount;
    }

    /**
     * Manual mapping: Entity → Response DTO
     * (In Phase 3 we might introduce MapStruct; for learning, manual is clearer.)
     */
    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(WalletTransaction tx, Long walletId) {
        return new TransactionResponse(
                tx.getId(),
                walletId,
                tx.getType(),
                tx.getAmount(),
                tx.getCreatedAt()
        );
    }
}
