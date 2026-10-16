package com.drippay.repository;

import com.drippay.domain.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndMonthYear(Long userId, String monthYear);
    Optional<Budget> findByUserIdAndCategoryIdAndMonthYear(Long userId, Long categoryId, String monthYear);
}
