package com.smarttourplanner.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import com.smarttourplanner.admin.AdminService.CatalogChanged;
import com.smarttourplanner.catalog.VersionedCache;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageRefresherTest {

    private final AtomicInteger version = new AtomicInteger(1);
    private final VersionedCache cache = new VersionedCache(version::get, Duration.ofHours(1), Duration.ofHours(1),
            Ticker.systemTicker());
    private final BlockingQueue<String> secretsReceived = new LinkedBlockingQueue<>();
    private HttpServer website;

    @BeforeEach
    void startWebsite() throws IOException {
        website = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        website.createContext("/hooks/catalog-changed", exchange -> {
            // Next.js drops a request that asks to upgrade to HTTP/2, so this does too.
            if (exchange.getRequestHeaders().containsKey("Upgrade")) {
                exchange.close();
                return;
            }
            secretsReceived.add(String.valueOf(exchange.getRequestHeaders().getFirst(PageRefresher.SECRET_HEADER)));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        website.start();
    }

    @AfterEach
    void stopWebsite() {
        website.stop(0);
    }

    @Test
    void refreshesThisServersCacheAtOnce() {
        assertThat(cache.get("answer", () -> "old")).isEqualTo("old");
        version.incrementAndGet();

        new PageRefresher(cache, null, null, Duration.ZERO).onCatalogChanged(new CatalogChanged());

        assertThat(cache.get("answer", () -> "new")).isEqualTo("new");
    }

    @Test
    void tellsTheWebsiteWithTheSharedSecret() throws InterruptedException {
        refresher(Duration.ZERO).onCatalogChanged(new CatalogChanged());

        assertThat(secretsReceived.poll(5, TimeUnit.SECONDS)).isEqualTo("test-secret");
    }

    @Test
    void waitsForOtherServersBeforeTellingTheWebsite() throws InterruptedException {
        refresher(Duration.ofMillis(700)).onCatalogChanged(new CatalogChanged());

        assertThat(secretsReceived.poll(300, TimeUnit.MILLISECONDS)).isNull();
        assertThat(secretsReceived.poll(5, TimeUnit.SECONDS)).isEqualTo("test-secret");
    }

    @Test
    void keepsWorkingWhenTheWebsiteIsDown() {
        website.stop(0);

        refresher(Duration.ZERO).onCatalogChanged(new CatalogChanged());

        assertThat(cache.get("answer", () -> "fine")).isEqualTo("fine");
    }

    private PageRefresher refresher(Duration wait) {
        URI hook = URI.create("http://localhost:%d/hooks/catalog-changed".formatted(website.getAddress().getPort()));
        return new PageRefresher(cache, hook, "test-secret", wait);
    }
}
