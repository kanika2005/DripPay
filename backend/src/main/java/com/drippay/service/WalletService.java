package com.drippay.service;

import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.dto.wallet.WalletLookupResponse;
import com.drippay.dto.wallet.WalletResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        return new WalletResponse(wallet.getId(), wallet.getPublicWalletId(), wallet.getBalance(), "INR", wallet.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public WalletLookupResponse lookup(String publicWalletId, Long requestingUserId) {
        Wallet wallet = walletRepository.findByPublicWalletId(publicWalletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getUserId().equals(requestingUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send to your own wallet id");
        }

        User user = userRepository.findById(wallet.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Mask email — show first name only for privacy
        String display = user.getFullName();
        return new WalletLookupResponse(wallet.getPublicWalletId(), display);
    }
}
