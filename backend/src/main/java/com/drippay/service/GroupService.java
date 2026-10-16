package com.drippay.service;

import com.drippay.domain.entity.*;
import com.drippay.dto.group.*;
import com.drippay.exception.ApiException;
import com.drippay.repository.*;
import com.drippay.util.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * SplitSmart — group expenses without auto-moving wallet funds.
 */
@Service
public class GroupService {

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupExpenseRepository expenseRepository;
    private final ExpenseShareRepository shareRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransferService transferService;
    private final SettlementRepository settlementRepository;

    public GroupService(ExpenseGroupRepository groupRepository,
                        GroupMemberRepository memberRepository,
                        GroupExpenseRepository expenseRepository,
                        ExpenseShareRepository shareRepository,
                        UserRepository userRepository,
                        WalletRepository walletRepository,
                        TransferService transferService,
                        SettlementRepository settlementRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.shareRepository = shareRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transferService = transferService;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        ExpenseGroup group = new ExpenseGroup();
        group.setName(request.name());
        group.setCreatedByUserId(userId);
        group = groupRepository.save(group);

        addMemberInternal(group.getId(), userId);
        for (Long memberId : request.memberUserIds()) {
            if (!memberId.equals(userId)) {
                addMemberInternal(group.getId(), memberId);
            }
        }
        return toGroupResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listGroups(Long userId) {
        return groupRepository.findAllForUser(userId).stream().map(this::toGroupResponse).toList();
    }

    @Transactional
    public GroupExpenseResponse addExpense(Long userId, Long groupId, GroupExpenseRequest request) {
        requireMember(groupId, userId);

        BigDecimal total = MoneyUtils.normalize(request.totalAmount());
        MoneyUtils.requirePositive(total);

        List<ShareInput> shares = request.shares();
        if (shares == null || shares.isEmpty()) {
            shares = splitEqually(total, memberRepository.findByGroupId(groupId));
        }

        BigDecimal sumShares = shares.stream()
                .map(s -> MoneyUtils.normalize(s.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumShares.compareTo(total) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Custom shares must equal the expense total");
        }

        GroupExpense expense = new GroupExpense();
        expense.setGroupId(groupId);
        expense.setPaidByUserId(request.paidByUserId() != null ? request.paidByUserId() : userId);
        expense.setTitle(request.title());
        expense.setTotalAmount(total);
        expense = expenseRepository.save(expense);

        for (ShareInput share : shares) {
            requireMember(groupId, share.userId());
            ExpenseShare es = new ExpenseShare();
            es.setGroupExpenseId(expense.getId());
            es.setUserId(share.userId());
            es.setShareAmount(MoneyUtils.normalize(share.amount()));
            shareRepository.save(es);
        }

        return new GroupExpenseResponse(expense.getId(), expense.getTitle(), expense.getTotalAmount(), expense.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> balances(Long groupId, Long userId) {
        requireMember(groupId, userId);

        Map<Long, BigDecimal> paid = new HashMap<>();
        Map<Long, BigDecimal> owed = new HashMap<>();

        for (GroupExpense expense : expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            paid.merge(expense.getPaidByUserId(), expense.getTotalAmount(), BigDecimal::add);
            for (ExpenseShare share : shareRepository.findByGroupExpenseId(expense.getId())) {
                owed.merge(share.getUserId(), share.getShareAmount(), BigDecimal::add);
            }
        }

        Map<Long, BigDecimal> net = new HashMap<>();
        for (GroupMember member : memberRepository.findByGroupId(groupId)) {
            BigDecimal p = paid.getOrDefault(member.getUserId(), BigDecimal.ZERO);
            BigDecimal o = owed.getOrDefault(member.getUserId(), BigDecimal.ZERO);
            net.put(member.getUserId(), p.subtract(o));
        }
        return net;
    }

    @Transactional
    public com.drippay.dto.transaction.TransactionResponse settle(Long fromUserId, Long groupId,
                                                                  SettleRequest request,
                                                                  String idempotencyKey, String ip) {
        requireMember(groupId, fromUserId);
        requireMember(groupId, request.toUserId());

        Wallet recipientWallet = walletRepository.findByUserId(request.toUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient wallet not found"));

        var transfer = new com.drippay.dto.transaction.TransferRequest(
                recipientWallet.getPublicWalletId(),
                request.amount(),
                request.note() != null ? request.note() : "SplitSmart settlement",
                "TRANSFER"
        );

        var tx = transferService.safeTransfer(fromUserId, transfer, idempotencyKey, ip);

        Settlement settlement = new Settlement();
        settlement.setGroupId(groupId);
        settlement.setFromUserId(fromUserId);
        settlement.setToUserId(request.toUserId());
        settlement.setAmount(MoneyUtils.normalize(request.amount()));
        settlement.setTransactionId(tx.id());
        settlementRepository.save(settlement);

        return tx;
    }

    private List<ShareInput> splitEqually(BigDecimal total, List<GroupMember> members) {
        int n = members.size();
        if (n == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Group has no members");
        BigDecimal base = total.divide(BigDecimal.valueOf(n), MoneyUtils.SCALE, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(n)));

        List<ShareInput> shares = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            BigDecimal amount = base;
            if (i == 0) {
                amount = amount.add(remainder);
            }
            shares.add(new ShareInput(members.get(i).getUserId(), amount));
        }
        return shares;
    }

    private void addMemberInternal(Long groupId, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User not found: " + userId);
        }
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            GroupMember m = new GroupMember();
            m.setGroupId(groupId);
            m.setUserId(userId);
            memberRepository.save(m);
        }
    }

    private void requireMember(Long groupId, Long userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not a group member");
        }
    }

    private GroupResponse toGroupResponse(ExpenseGroup g) {
        return new GroupResponse(g.getId(), g.getName(), g.getCreatedAt());
    }
}
