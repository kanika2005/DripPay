package com.drippay.repository;

import com.drippay.domain.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    @Query("""
        SELECT g FROM ExpenseGroup g
        WHERE g.id IN (SELECT m.groupId FROM GroupMember m WHERE m.userId = :userId)
        ORDER BY g.createdAt DESC
        """)
    List<ExpenseGroup> findAllForUser(@Param("userId") Long userId);
}
