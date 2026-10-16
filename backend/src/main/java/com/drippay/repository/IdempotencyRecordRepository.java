package com.drippay.repository;

import com.drippay.domain.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
