package com.drippay.repository;

import com.drippay.domain.entity.GroupExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, Long> {
    List<GroupExpense> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
