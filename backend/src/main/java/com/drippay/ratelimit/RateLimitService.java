package com.drippay.ratelimit;

import com.drippay.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RateLimitService {

    private final RateLimitCounterRepository repository;
    private final int loginPerMinute;
    private final int transferPerMinute;

    public RateLimitService(RateLimitCounterRepository repository,
                            @Value("${drippay.rate-limit.login-per-minute:20}") int loginPerMinute,
                            @Value("${drippay.rate-limit.transfer-per-minute:30}") int transferPerMinute) {
        this.repository = repository;
        this.loginPerMinute = loginPerMinute;
        this.transferPerMinute = transferPerMinute;
    }

    @Transactional
    public void checkLogin(String email) {
        bump(null, "login:" + email.toLowerCase(), loginPerMinute);
    }

    @Transactional
    public void checkTransfer(Long userId) {
        bump(userId, "transfer", transferPerMinute);
    }

    @Transactional
    public void checkInsightsChat(Long userId) {
        bump(userId, "insights_chat", 30);
    }

    private void bump(Long userId, String actionKey, int maxPerWindow) {
        Instant windowStart = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        repository.incrementCounter(userId, actionKey, windowStart);
        Integer current = userId == null
                ? repository.countForGlobal(actionKey, windowStart)
                : repository.countForUser(userId, actionKey, windowStart);
        if (current != null && current > maxPerWindow) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded — try again shortly", 60);
        }
    }
}
