package com.drippay.service;

import com.drippay.domain.entity.ExpenseGroup;
import com.drippay.domain.entity.GroupMember;
import com.drippay.dto.group.GroupExpenseRequest;
import com.drippay.exception.ApiException;
import com.drippay.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceSplitTest {

    @Mock ExpenseGroupRepository groupRepository;
    @Mock GroupMemberRepository memberRepository;
    @Mock GroupExpenseRepository expenseRepository;
    @Mock ExpenseShareRepository shareRepository;
    @Mock UserRepository userRepository;
    @Mock WalletRepository walletRepository;
    @Mock TransferService transferService;
    @Mock SettlementRepository settlementRepository;
    @Mock NotificationService notificationService;

    @InjectMocks GroupService groupService;

    @Test
    void customSharesMustEqualTotal() {
        when(memberRepository.existsByGroupIdAndUserId(1L, 1L)).thenReturn(true);

        GroupExpenseRequest req = new GroupExpenseRequest(
                "Dinner",
                new BigDecimal("100.00"),
                1L,
                List.of(new com.drippay.dto.group.ShareInput(1L, new BigDecimal("40")),
                        new com.drippay.dto.group.ShareInput(2L, new BigDecimal("40")))
        );

        assertThrows(ApiException.class, () -> groupService.addExpense(1L, 1L, req));
    }
}
