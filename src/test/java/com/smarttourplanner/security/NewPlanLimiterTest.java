package com.smarttourplanner.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NewPlanLimiterTest {

    @Test
    void allowsRequestsUpToTheLimit() {
        NewPlanLimiter limiter = new NewPlanLimiter(3, 100);

        assertThatCode(() -> {
            limiter.check("client");
            limiter.check("client");
            limiter.check("client");
        }).doesNotThrowAnyException();
    }

    @Test
    void blocksTheClientPastItsLimitAndSaysWhenToRetry() {
        NewPlanLimiter limiter = new NewPlanLimiter(1, 100);
        limiter.check("client");

        assertThatThrownBy(() -> limiter.check("client"))
                .isInstanceOfSatisfying(RateLimitExceededException.class,
                        limit -> assertThat(limit.retryAfterSeconds()).isPositive());
    }

    @Test
    void keepsClientsSeparate() {
        NewPlanLimiter limiter = new NewPlanLimiter(1, 100);
        limiter.check("first");

        assertThatCode(() -> limiter.check("second")).doesNotThrowAnyException();
    }

    @Test
    void appliesASiteWideLimitAcrossClients() {
        NewPlanLimiter limiter = new NewPlanLimiter(10, 2);
        limiter.check("first");
        limiter.check("second");

        assertThatThrownBy(() -> limiter.check("third")).isInstanceOf(RateLimitExceededException.class);
    }
}
