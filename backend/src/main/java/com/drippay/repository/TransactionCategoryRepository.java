package com.drippay.repository;

import com.drippay.domain.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
    Optional<TransactionCategory> findByCode(String code);
}
