package com.drippay.controller;

import com.drippay.dto.group.*;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.security.SecurityUtils;
import com.drippay.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(SecurityUtils.currentUser().getId(), request);
    }

    @GetMapping
    public List<GroupResponse> list() {
        return groupService.listGroups(SecurityUtils.currentUser().getId());
    }

    @PostMapping("/{groupId}/expenses")
    public GroupExpenseResponse addExpense(@PathVariable Long groupId,
                                           @Valid @RequestBody GroupExpenseRequest request) {
        return groupService.addExpense(SecurityUtils.currentUser().getId(), groupId, request);
    }

    @GetMapping("/{groupId}/expenses/{expenseId}")
    public GroupExpenseDetailResponse getExpense(@PathVariable Long groupId, @PathVariable Long expenseId) {
        return groupService.getExpense(SecurityUtils.currentUser().getId(), groupId, expenseId);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> members(@PathVariable Long groupId) {
        return groupService.listMembers(SecurityUtils.currentUser().getId(), groupId);
    }

    @PostMapping("/{groupId}/members")
    public GroupMemberResponse addMember(@PathVariable Long groupId,
                                         @Valid @RequestBody AddGroupMemberRequest request) {
        return groupService.addMember(SecurityUtils.currentUser().getId(), groupId, request);
    }

    @GetMapping("/{groupId}/balances")
    public Map<Long, BigDecimal> balances(@PathVariable Long groupId) {
        return groupService.balances(groupId, SecurityUtils.currentUser().getId());
    }

    @PostMapping("/{groupId}/settlements")
    public TransactionResponse settle(@PathVariable Long groupId,
                                      @Valid @RequestBody SettleRequest request,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                      HttpServletRequest http) {
        return groupService.settle(SecurityUtils.currentUser().getId(), groupId, request, idempotencyKey, http.getRemoteAddr());
    }
}
