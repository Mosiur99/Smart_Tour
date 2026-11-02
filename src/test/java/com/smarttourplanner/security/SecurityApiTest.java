package com.smarttourplanner.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.support.TestDatabaseConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "smarttour.catalog.version-check-interval=0s",
            "smarttour.limits.new-plans-per-client-per-minute=2"
        })
@Import(TestDatabaseConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityApiTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @BeforeAll
    void publishTestTrip() {
        jdbc.sql("UPDATE destination SET status = 'PUBLISHED', lat = 24.89, lng = 91.87 WHERE slug = 'sylhet'").update();
        jdbc.sql("""
                UPDATE transport_leg SET status = 'PUBLISHED', duration_min = 420, checked_at = current_date,
                    source_url = 'https://example.com'
                WHERE mode = 'BUS'
                  AND (from_hub_id = (SELECT id FROM hub WHERE slug = 'sylhet')
                       OR to_hub_id = (SELECT id FROM hub WHERE slug = 'sylhet'))
                """).update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();
    }

    @Test
    void apiResponsesCarrySecurityHeaders() throws Exception {
        HttpResponse<String> response = get("/api/v1/hubs");

        assertThat(response.headers().firstValue("Content-Security-Policy")).contains("default-src 'none'; frame-ancestors 'none'");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().firstValue("X-Frame-Options")).contains("DENY");
        assertThat(response.headers().firstValue("Strict-Transport-Security")).isPresent();
        assertThat(response.headers().firstValue("Referrer-Policy")).isPresent();
    }

    @Test
    void pagesAllowOnlyTheirOwnAssetSources() throws Exception {
        String policy = get("/").headers().firstValue("Content-Security-Policy").orElseThrow();

        assertThat(policy).contains("script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net");
        assertThat(policy).contains("object-src 'none'").contains("frame-ancestors 'none'");
    }

    @Test
    void errorsRevealNoInternalDetails() throws Exception {
        HttpResponse<String> response = post("{broken");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).doesNotContain("Exception").doesNotContain("trace").doesNotContain("org.springframework");
    }

    @Test
    void limitsNewPlansButNotExistingOnes() throws Exception {
        assertThat(post(trip(7)).statusCode()).isEqualTo(201);
        assertThat(post(trip(8)).statusCode()).isEqualTo(201);

        HttpResponse<String> blocked = post(trip(9));

        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();
        assertThat(blocked.headers().firstValue("Content-Type").orElseThrow()).contains("application/problem+json");
        assertThat(post(trip(7)).statusCode()).isEqualTo(200);
    }

    @Test
    void refusesOversizedRequests() throws Exception {
        String large = "{\"padding\":\"" + "a".repeat(5_000) + "\"}";

        assertThat(post(large).statusCode()).isEqualTo(413);
    }

    @Test
    void givesAdminSavesMoreRoomButStillALimit() throws Exception {
        String longNote = "{\"revision\": 0, \"values\": {\"note\": \"%s\"}}";

        // Within the admin limit, so the request reaches the login check.
        assertThat(put("/api/v1/admin/hotels/1", longNote.formatted("ন".repeat(3_000))).statusCode()).isEqualTo(401);
        assertThat(put("/api/v1/admin/hotels/1", longNote.formatted("x".repeat(40_000))).statusCode()).isEqualTo(413);
        assertThat(put("/api/v1/plans/x", longNote.formatted("x".repeat(5_000))).statusCode()).isEqualTo(413);
    }

    @Test
    void refusesRequestsOfUnknownSize() throws Exception {
        HttpRequest chunked = request("/api/v1/plans")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))))
                .build();

        assertThat(client.send(chunked, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(411);
    }

    @Test
    void refusesMethodsTheApiDoesntSupport() throws Exception {
        HttpRequest delete = request("/api/v1/hubs").DELETE().build();

        assertThat(client.send(delete, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(405);
    }

    @Test
    void refusesNonJsonPlanRequests() throws Exception {
        HttpRequest form = request("/api/v1/plans")
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("hello"))
                .build();

        assertThat(client.send(form, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(415);
    }

    @Test
    void sessionCookieCantBeReadByScripts() throws Exception {
        HttpResponse<String> response = client.send(
                request("/plan/save")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("districtId=rajshahi&originCity=Dhaka"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(cookie).contains("HttpOnly").containsIgnoringCase("SameSite=Lax");
    }

    private static String trip(int days) {
        return """
                {"originHub":"dhaka","destination":"sylhet","days":%d,"people":2,"budget":"MID","month":12}
                """.formatted(days);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String json) throws IOException, InterruptedException {
        return client.send(
                request("/api/v1/plans")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String json) throws IOException, InterruptedException {
        return client.send(
                request(path).header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }
}
