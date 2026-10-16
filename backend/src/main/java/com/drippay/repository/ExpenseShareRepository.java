package com.drippay.repository;

import com.drippay.domain.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    List<ExpenseShare> findByGroupExpenseId(Long groupExpenseId);
}
