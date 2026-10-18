package com.drippay.repository;

import com.drippay.domain.entity.RazorpayOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {
    Optional<RazorpayOrder> findByRazorpayOrderId(String razorpayOrderId);
    Optional<RazorpayOrder> findByRazorpayOrderIdAndUserId(String razorpayOrderId, Long userId);
}
