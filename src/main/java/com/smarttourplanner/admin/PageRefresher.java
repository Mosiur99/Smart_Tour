package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminService.CatalogChanged;
import com.smarttourplanner.catalog.VersionedCache;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Brings visitors the new data after a publish. This server's cache refreshes at once. The website is
 * told to rebuild its pages once every API server has seen the new version, so a page can't be
 * rebuilt from a server that still has the old data. Pages also rebuild on their own every 10 minutes,
 * so a missed call only delays the change.
 */
class PageRefresher {

    static final String SECRET_HEADER = "X-Refresh-Secret";

    private static final Logger log = LoggerFactory.getLogger(PageRefresher.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final VersionedCache cache;
    private final @Nullable URI websiteHook;
    private final @Nullable String secret;
    private final Duration waitForOtherServers;
    // HTTP/1.1, because the default tries an HTTP/2 upgrade that the Node.js server drops.
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();

    PageRefresher(VersionedCache cache, @Nullable URI websiteHook, @Nullable String secret,
            Duration waitForOtherServers) {
        this.cache = cache;
        this.websiteHook = websiteHook;
        this.secret = secret;
        this.waitForOtherServers = waitForOtherServers;
    }

    /** Runs only after the change is committed, so nothing refreshes for a change that was rolled back. */
    @TransactionalEventListener
    void onCatalogChanged(CatalogChanged change) {
        cache.forgetVersion();
        if (websiteHook != null && secret != null) {
            Thread.ofVirtual().name("page-refresh").start(this::tellWebsite);
        }
    }

    private void tellWebsite() {
        try {
            Thread.sleep(waitForOtherServers);
            HttpRequest request = HttpRequest.newBuilder(websiteHook)
                    .timeout(TIMEOUT)
                    .header(SECRET_HEADER, secret)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status != 200) {
                log.warn("The website answered {} when asked to refresh its pages", status);
            }
        } catch (IOException e) {
            log.warn("Couldn't ask the website to refresh its pages", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
