package com.drippay.ratelimit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rate_limit_counters")
public class RateLimitCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action_key", nullable = false, length = 64)
    private String actionKey;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private int count;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
