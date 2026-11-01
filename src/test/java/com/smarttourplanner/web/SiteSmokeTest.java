package com.smarttourplanner.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** Runs the real server and renders real JSP pages. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SiteSmokeTest {

    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${local.server.port}")
    private int port;

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
    void healthEndpointResponds() throws Exception {
        assertThat(get("/health").statusCode()).isEqualTo(200);
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
