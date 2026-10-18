package com.drippay.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {
    List<WebhookDelivery> findTop30ByStatusOrderByCreatedAtAsc(String status);
}
