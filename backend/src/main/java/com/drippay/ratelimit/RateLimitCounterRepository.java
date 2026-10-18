package com.drippay.ratelimit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RateLimitCounterRepository extends JpaRepository<RateLimitCounter, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO rate_limit_counters (user_id, action_key, window_start, count)
            VALUES (:userId, :actionKey, :windowStart, 1)
            ON DUPLICATE KEY UPDATE count = count + 1
            """, nativeQuery = true)
    int incrementCounter(@Param("userId") Long userId,
                         @Param("actionKey") String actionKey,
                         @Param("windowStart") Instant windowStart);

    @Query(value = """
            SELECT count FROM rate_limit_counters
            WHERE user_id IS NULL AND action_key = :actionKey AND window_start = :windowStart
            """, nativeQuery = true)
    Integer countForGlobal(@Param("actionKey") String actionKey, @Param("windowStart") Instant windowStart);

    @Query(value = """
            SELECT count FROM rate_limit_counters
            WHERE user_id = :userId AND action_key = :actionKey AND window_start = :windowStart
            """, nativeQuery = true)
    Integer countForUser(@Param("userId") Long userId,
                         @Param("actionKey") String actionKey,
                         @Param("windowStart") Instant windowStart);
}
