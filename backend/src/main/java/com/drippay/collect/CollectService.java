package com.drippay.collect;

import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.dto.collect.CollectProfileResponse;
import com.drippay.dto.collect.HandleLookupResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class CollectService {

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,30}@drippay$");
    private static final String HANDLE_SUFFIX = "@drippay";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public CollectService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public CollectProfileResponse myCollectProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        String handle = ensureHandle(user);
        String qrPayload = "drippay://collect?handle=" + handle;
        return new CollectProfileResponse(handle, qrPayload);
    }

    @Transactional(readOnly = true)
    public HandleLookupResponse resolveHandle(String rawHandle, Long requesterUserId) {
        String handle = normalizeHandle(rawHandle);
        User recipient = userRepository.findByDripHandle(handle)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Handle not found"));
        if (recipient.getId().equals(requesterUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot pay yourself via collect");
        }
        Wallet wallet = walletRepository.findByUserId(recipient.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        return new HandleLookupResponse(handle, recipient.getFullName(), wallet.getPublicWalletId());
    }

    @Transactional
    public CollectProfileResponse claimHandle(Long userId, String requestedHandle) {
        String handle = normalizeHandle(requestedHandle);
        if (!HANDLE_PATTERN.matcher(handle).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Handle must be like name@drippay (lowercase, 3–32 chars before @)");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (userRepository.existsByDripHandle(handle) && !handle.equals(user.getDripHandle())) {
            throw new ApiException(HttpStatus.CONFLICT, "Handle already taken");
        }
        user.setDripHandle(handle);
        userRepository.save(user);
        return new CollectProfileResponse(handle, "drippay://collect?handle=" + handle);
    }

    public String normalizeHandle(String raw) {
        if (raw == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Handle required");
        }
        String h = raw.trim().toLowerCase(Locale.ROOT);
        if (!h.endsWith(HANDLE_SUFFIX)) {
            h = h + HANDLE_SUFFIX;
        }
        return h;
    }

    @Transactional
    public String ensureHandle(User user) {
        if (user.getDripHandle() != null && !user.getDripHandle().isBlank()) {
            return user.getDripHandle();
        }
        String base = user.getEmail().split("@")[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (base.length() < 2) {
            base = "user" + user.getId();
        }
        String candidate = base + HANDLE_SUFFIX;
        int n = 0;
        while (userRepository.existsByDripHandle(candidate)) {
            n++;
            candidate = base + n + HANDLE_SUFFIX;
        }
        user.setDripHandle(candidate);
        userRepository.save(user);
        return candidate;
    }

    public User userForHandle(String handle) {
        return userRepository.findByDripHandle(normalizeHandle(handle))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Handle not found"));
    }
}
