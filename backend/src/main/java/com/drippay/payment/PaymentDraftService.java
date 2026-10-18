package com.drippay.payment;

import com.drippay.collect.CollectService;
import com.drippay.domain.entity.PaymentDraft;
import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.dto.payment.PaymentDraftResponse;
import com.drippay.dto.transaction.TransferRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.PaymentDraftRepository;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletRepository;
import com.drippay.service.TransferPinService;
import com.drippay.service.TransferService;
import com.drippay.util.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PaymentDraftService {

    private final PaymentDraftRepository draftRepository;
    private final CollectService collectService;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransferPinService transferPinService;
    private final TransferService transferService;

    public PaymentDraftService(PaymentDraftRepository draftRepository,
                               CollectService collectService,
                               WalletRepository walletRepository,
                               UserRepository userRepository,
                               TransferPinService transferPinService,
                               TransferService transferService) {
        this.draftRepository = draftRepository;
        this.collectService = collectService;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.transferPinService = transferPinService;
        this.transferService = transferService;
    }

    @Transactional
    public PaymentDraftResponse create(Long userId, String recipientHandle, BigDecimal amount, String note, String source) {
        BigDecimal normalized = MoneyUtils.normalize(amount);
        User recipient = collectService.userForHandle(recipientHandle);
        if (recipient.getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot draft payment to yourself");
        }
        Wallet wallet = walletRepository.findByUserId(recipient.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient wallet missing"));
        PaymentDraft draft = new PaymentDraft();
        draft.setUserId(userId);
        draft.setRecipientHandle(collectService.normalizeHandle(recipientHandle));
        draft.setRecipientWalletId(wallet.getPublicWalletId());
        draft.setAmount(normalized);
        draft.setNote(note);
        draft.setSource(source != null ? source : "USER");
        draft.setStatus("PENDING");
        draft.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        draft = draftRepository.save(draft);
        return toResponse(draft);
    }

    @Transactional(readOnly = true)
    public PaymentDraftResponse get(Long userId, Long draftId) {
        PaymentDraft draft = draftRepository.findByIdAndUserId(draftId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found"));
        return toResponse(draft);
    }

    @Transactional
    public TransactionResponse confirm(Long userId, Long draftId, String pin, String ip) {
        PaymentDraft draft = draftRepository.findByIdAndUserId(draftId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (!"PENDING".equals(draft.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Draft is not pending");
        }
        if (draft.getExpiresAt().isBefore(Instant.now())) {
            draft.setStatus("EXPIRED");
            draftRepository.save(draft);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Draft expired");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        transferPinService.verifyPin(user, pin);

        TransferRequest request = new TransferRequest(
                draft.getRecipientWalletId(),
                draft.getAmount(),
                draft.getNote(),
                null
        );
        String idempotencyKey = "draft-" + draft.getId();
        TransactionResponse tx = transferService.safeTransfer(userId, request, idempotencyKey, ip);
        draft.setStatus("CONFIRMED");
        draftRepository.save(draft);
        return tx;
    }

    private PaymentDraftResponse toResponse(PaymentDraft draft) {
        return new PaymentDraftResponse(
                draft.getId(),
                draft.getRecipientHandle(),
                draft.getRecipientWalletId(),
                draft.getAmount(),
                draft.getNote(),
                draft.getStatus(),
                draft.getExpiresAt()
        );
    }
}
