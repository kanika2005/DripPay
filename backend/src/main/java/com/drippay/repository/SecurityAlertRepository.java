package com.drippay.repository;

import com.drippay.domain.entity.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SecurityAlert> findAllByOrderByCreatedAtDesc();
}
