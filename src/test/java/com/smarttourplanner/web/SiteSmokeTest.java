package com.smarttourplanner.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.support.TestDatabaseConfig;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Runs the real server and renders real JSP pages. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "smarttour.catalog.version-check-interval=0s")
@Import(TestDatabaseConfig.class)
class SiteSmokeTest {

    private static final String SCRIPT = "<script>alert(1)</script>";
    private static final String ENCODED_SCRIPT = URLEncoder.encode(SCRIPT, StandardCharsets.UTF_8);

    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void homePageListsDistricts() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Start Planning").contains("Rajshahi");
    }

    @Test
    void districtPageRenders() throws Exception {
        HttpResponse<String> response = get("/district/rajshahi");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Rajshahi");
    }

    @Test
    void tourBuilderRenders() throws Exception {
        HttpResponse<String> response = get("/plan/build/rajshahi?origin=Dhaka");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Dhaka").contains("Rajshahi");
    }

    @Test
    void savedPlanRendersAndExportsAsPdf() throws Exception {
        HttpResponse<String> summary = post("/plan/save", "districtId=rajshahi&originCity=Dhaka&travelers=2&nights=2");

        assertThat(summary.statusCode()).isEqualTo(200);
        assertThat(summary.body()).contains("Estimate Cost").contains("Rajshahi");

        HttpResponse<byte[]> pdf = client.send(
                request("/plan/export/pdf").GET().build(), HttpResponse.BodyHandlers.ofByteArray());

        assertThat(pdf.statusCode()).isEqualTo(200);
        assertThat(pdf.headers().firstValue("Content-Type")).contains("application/pdf");
    }

    @Test
    void districtPageShowsDatabaseChangesWithoutARedeploy() throws Exception {
        jdbc.sql("UPDATE destination SET travel_note = 'Live database note' WHERE slug = 'khulna'").update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();

        assertThat(get("/district/khulna").body()).contains("Live database note");
    }

    @Test
    void apiAnswersWithNotModifiedWhenDataIsUnchanged() throws Exception {
        String etag = get("/api/v1/hubs").headers().firstValue("ETag").orElseThrow();

        HttpResponse<String> repeat = client.send(
                request("/api/v1/hubs").header("If-None-Match", etag).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(repeat.statusCode()).isEqualTo(304);
    }

    @Test
    void healthEndpointResponds() throws Exception {
        assertThat(get("/health").statusCode()).isEqualTo(200);
    }

    @Test
    void unknownDistrictReturnsNotFoundWithoutEchoingInput() throws Exception {
        HttpResponse<String> response = get("/district/atlantis");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("couldn't find that district").doesNotContain("atlantis");
    }

    @Test
    void homePageIgnoresTheErrorParameter() throws Exception {
        assertThat(get("/?error=" + ENCODED_SCRIPT).body()).doesNotContain(SCRIPT);
    }

    @Test
    void tourBuilderRejectsScriptInTheOrigin() throws Exception {
        HttpResponse<String> response = get("/plan/build/rajshahi?origin=" + ENCODED_SCRIPT);

        assertThat(response.body()).doesNotContain(SCRIPT).contains("Dhaka");
    }

    @Test
    void savedPlanRejectsScriptInTheOrigin() throws Exception {
        HttpResponse<String> response = post("/plan/save", "districtId=rajshahi&originCity=" + ENCODED_SCRIPT);

        assertThat(response.body()).doesNotContain(SCRIPT);
    }

    @Test
    void summaryListsCostsThatAreNotIncluded() throws Exception {
        HttpResponse<String> response = post("/plan/save", "districtId=rangamati&originCity=Dhaka");

        assertThat(response.body()).contains("Not included: Hotel");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String form) throws IOException, InterruptedException {
        HttpRequest post = request(path)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return client.send(post, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }
}
