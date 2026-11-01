package com.smarttourplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class VersionedCacheTest {

    private static final Duration VERSION_CHECK = Duration.ofSeconds(5);
    private static final Duration ENTRY_LIFETIME = Duration.ofMinutes(10);

    private final AtomicInteger version = new AtomicInteger(1);
    private final AtomicInteger loads = new AtomicInteger();
    private final AtomicLong clock = new AtomicLong();
    private final VersionedCache cache =
            new VersionedCache(version::get, VERSION_CHECK, ENTRY_LIFETIME, clock::get);

    @Test
    void loadsOnlyOnceWhileTheEntryIsFresh() {
        load();
        load();

        assertThat(loads).hasValue(1);
    }

    @Test
    void keepsKeysSeparate() {
        cache.get("first", () -> "one");

        assertThat(cache.get("second", () -> "two")).isEqualTo("two");
    }

    @Test
    void reloadsAfterAPublishOnceTheVersionCheckRuns() {
        load();
        version.incrementAndGet();
        advance(VERSION_CHECK.plusSeconds(1));

        load();

        assertThat(loads).hasValue(2);
    }

    @Test
    void reloadsWhenTheEntryExpires() {
        load();
        advance(ENTRY_LIFETIME.plusSeconds(1));

        load();

        assertThat(loads).hasValue(2);
    }

    @Test
    void doesNotCacheMissingResults() {
        cache.get("missing", () -> {
            loads.incrementAndGet();
            return null;
        });
        cache.get("missing", () -> {
            loads.incrementAndGet();
            return null;
        });

        assertThat(loads).hasValue(2);
    }

    private void load() {
        cache.get("key", loads::incrementAndGet);
    }

    private void advance(Duration duration) {
        clock.addAndGet(duration.toNanos());
    }
}
