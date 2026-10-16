package com.drippay.service;

import com.drippay.domain.entity.*;
import com.drippay.domain.enums.TransactionStatus;
import com.drippay.domain.enums.TransactionType;
import com.drippay.dto.transaction.TransferRequest;
import com.drippay.exception.ApiException;
import com.drippay.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository transactionRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock TransactionCategoryRepository categoryRepository;
    @Mock FraudWatchService fraudWatchService;
    @Mock AuditService auditService;

    @InjectMocks TransferService transferService;

    Wallet sender;
    Wallet recipient;

    @BeforeEach
    void setup() {
        sender = new Wallet();
        setId(sender, 2L);
        sender.setUserId(10L);
        sender.setPublicWalletId("DRIPSENDER1");
        sender.setBalance(new BigDecimal("1000.0000"));

        recipient = new Wallet();
        setId(recipient, 3L);
        recipient.setUserId(11L);
        recipient.setPublicWalletId("DRIPRECV01");
        recipient.setBalance(new BigDecimal("50.0000"));
    }

    @Test
    void transfer_insufficientFunds() {
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByPublicWalletId("DRIPRECV01")).thenReturn(Optional.of(recipient));
        when(categoryRepository.findByCode("TRANSFER")).thenReturn(Optional.of(category()));

        when(walletRepository.findByIdForUpdate(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (id.equals(2L)) return Optional.of(sender);
            if (id.equals(3L)) return Optional.of(recipient);
            return Optional.empty();
        });

        TransferRequest req = new TransferRequest("DRIPRECV01", new BigDecimal("5000"), "test", null);
        assertThrows(ApiException.class, () -> transferService.safeTransfer(10L, req, null, "127.0.0.1"));
        verify(fraudWatchService).onFailedTransfer(10L);
    }

    @Test
    void transfer_sameWalletRejected() {
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByPublicWalletId("DRIPSENDER1")).thenReturn(Optional.of(sender));

        TransferRequest req = new TransferRequest("DRIPSENDER1", new BigDecimal("10"), null, null);
        assertThrows(ApiException.class, () -> transferService.safeTransfer(10L, req, null, "127.0.0.1"));
    }

    private TransactionCategory category() {
        return mock(TransactionCategory.class, invocation -> {
            if ("getId".equals(invocation.getMethod().getName())) return 6L;
            if ("getCode".equals(invocation.getMethod().getName())) return "TRANSFER";
            return null;
        });
    }

    private static void setId(Wallet w, Long id) {
        try {
            var f = Wallet.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(w, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
