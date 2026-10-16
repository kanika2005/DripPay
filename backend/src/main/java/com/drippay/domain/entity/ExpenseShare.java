package com.drippay.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_shares")
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_expense_id", nullable = false)
    private Long groupExpenseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "share_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal shareAmount;

    public Long getId() { return id; }
    public Long getGroupExpenseId() { return groupExpenseId; }
    public void setGroupExpenseId(Long groupExpenseId) { this.groupExpenseId = groupExpenseId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal shareAmount) { this.shareAmount = shareAmount; }
}
