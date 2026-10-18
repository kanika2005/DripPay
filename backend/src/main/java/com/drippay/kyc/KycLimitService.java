package com.drippay.kyc;

import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.domain.enums.KycTier;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.exception.ApiException;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
public class KycLimitService {

    private static final BigDecimal MIN_KYC_MAX_BALANCE = BigDecimal.valueOf(10_000);
    private static final BigDecimal MIN_KYC_MAX_TXN = BigDecimal.valueOf(5_000);
    private static final BigDecimal MIN_KYC_MONTHLY_OUT = BigDecimal.valueOf(20_000);

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;

    public KycLimitService(UserRepository userRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public void assertTransferAllowed(Long userId, Wallet senderWallet, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getKycTier() == KycTier.FULL_KYC) {
            return;
        }
        if (amount.compareTo(MIN_KYC_MAX_TXN) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "MIN_KYC per-transfer limit is ₹" + MIN_KYC_MAX_TXN);
        }
        if (senderWallet.getBalance().compareTo(MIN_KYC_MAX_BALANCE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "MIN_KYC wallet balance cap is ₹" + MIN_KYC_MAX_BALANCE + " — upgrade KYC");
        }
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal spent = transactionRepository.sumSentSettled(senderWallet.getId(), from, to);
        if (spent.add(amount).compareTo(MIN_KYC_MONTHLY_OUT) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "MIN_KYC monthly outgoing limit is ₹" + MIN_KYC_MONTHLY_OUT);
        }
    }
}
