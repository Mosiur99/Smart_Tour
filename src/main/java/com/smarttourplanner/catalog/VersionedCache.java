package com.smarttourplanner.catalog;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Keeps catalog results in memory, keyed by catalog version. Publishing raises the version, so old
 * entries stop matching. Each server rechecks the version briefly, so all servers catch up quickly.
 */
public class VersionedCache {

    private static final String VERSION_KEY = "version";
    private static final long MAX_ENTRIES = 2_000;

    private final LoadingCache<String, Integer> version;
    private final Cache<String, Object> entries;

    public VersionedCache(IntSupplier versionSource, Duration versionCheckInterval, Duration entryLifetime, Ticker ticker) {
        this.version = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(versionCheckInterval)
                .build(key -> versionSource.getAsInt());
        this.entries = Caffeine.newBuilder()
                .ticker(ticker)
                .maximumSize(MAX_ENTRIES)
                .expireAfterWrite(entryLifetime)
                .build();
    }

    /** Rereads the version on the next lookup, so a change made on this server shows at once. */
    public void forgetVersion() {
        version.invalidateAll();
    }

    /** A loader that returns null is not cached. A loader must not call this cache itself. */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader) {
        return (T) entries.get(version.get(VERSION_KEY) + ":" + key, ignored -> loader.get());
    }
}
