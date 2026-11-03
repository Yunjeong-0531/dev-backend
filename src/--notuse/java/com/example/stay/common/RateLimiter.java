package com.example.stay.common;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 슬라이딩 윈도우 방식의 단순 in-memory rate limiter.
 * 서버가 여러 대이면 인스턴스별로 따로 집계되므로, 운영에서는 Redis 기반으로 교체할 것.
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> store = new ConcurrentHashMap<>();

    public void check(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Deque<Long> hits = store.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
                hits.pollFirst();
            }
            if (hits.size() >= maxRequests) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
            hits.addLast(now);
        }
    }

    @Scheduled(fixedDelay = 600_000)
    void cleanup() {
        long cutoff = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        store.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || last < cutoff;
            }
        });
    }
}
