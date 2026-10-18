package com.drippay.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentConversationRepository extends JpaRepository<AgentConversation, Long> {
    List<AgentConversation> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
