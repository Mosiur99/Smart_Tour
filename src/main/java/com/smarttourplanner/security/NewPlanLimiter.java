package com.smarttourplanner.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Limits new plans only. Plans that already exist cost almost nothing, and many people in
 * Bangladesh share one mobile IP address, so limiting every request would block real travellers.
 */
public class NewPlanLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final long MAX_TRACKED_CLIENTS = 100_000;

    private final int perClientPerMinute;
    private final Bucket everyone;
    private final Cache<String, Bucket> clients = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_CLIENTS)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public NewPlanLimiter(int perClientPerMinute, int totalPerMinute) {
        this.perClientPerMinute = perClientPerMinute;
        this.everyone = bucket(totalPerMinute);
    }

    public void check(String client) {
        consume(clients.get(client, ignored -> bucket(perClientPerMinute)));
        consume(everyone);
    }

    private void consume(Bucket bucket) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            throw new RateLimitExceededException(seconds);
        }
    }

    private static Bucket bucket(int perMinute) {
        return Bucket.builder().addLimit(limit -> limit.capacity(perMinute).refillGreedy(perMinute, WINDOW)).build();
    }
}
