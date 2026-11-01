package com.smarttourplanner.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.support.TestDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * A quick speed check on one machine. It isn't the launch load test. Run it with:
 * mvn test -Dtest=ApiSpeedCheck -DexcludedGroups=none
 */
@Tag("performance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestDatabaseConfig.class)
class ApiSpeedCheck {

    private static final int CLIENTS = 16;
    private static final int REQUESTS_PER_CLIENT = 500;
    private static final List<String> PATHS = List.of(
            "/api/v1/hubs",
            "/api/v1/destinations?limit=20",
            "/api/v1/destinations/rajshahi",
            "/api/v1/destinations/coxs-bazar",
            "/api/v1/destinations?q=sylhet");

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void measuresCachedReads() throws Exception {
        for (String path : PATHS) {
            timeRequest(path);
        }

        List<Long> times = Collections.synchronizedList(new ArrayList<>());
        long start = System.nanoTime();
        try (AutoClosingPool pool = new AutoClosingPool(CLIENTS)) {
            List<Future<?>> clients = new ArrayList<>();
            for (int c = 0; c < CLIENTS; c++) {
                clients.add(pool.executor.submit(() -> {
                    for (int r = 0; r < REQUESTS_PER_CLIENT; r++) {
                        times.add(timeRequest(PATHS.get(r % PATHS.size())));
                    }
                }));
            }
            for (Future<?> running : clients) {
                running.get();
            }
        }
        double seconds = (System.nanoTime() - start) / 1e9;

        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        System.out.printf("SPEED requests=%d rps=%.0f p50=%.1fms p95=%.1fms p99=%.1fms%n",
                sorted.size(), sorted.size() / seconds, millis(sorted, 0.50), millis(sorted, 0.95), millis(sorted, 0.99));
        assertThat(millis(sorted, 0.95)).isLessThan(200);
    }

    private long timeRequest(String path) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isEqualTo(200);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return System.nanoTime() - start;
    }

    private static double millis(List<Long> sorted, double percentile) {
        return sorted.get((int) (sorted.size() * percentile)) / 1e6;
    }

    private static final class AutoClosingPool implements AutoCloseable {
        private final ExecutorService executor;

        private AutoClosingPool(int size) {
            executor = Executors.newFixedThreadPool(size);
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }
}
