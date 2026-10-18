package com.drippay.repository;

import com.drippay.domain.entity.PaymentDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentDraftRepository extends JpaRepository<PaymentDraft, Long> {
    Optional<PaymentDraft> findByIdAndUserId(Long id, Long userId);
}
