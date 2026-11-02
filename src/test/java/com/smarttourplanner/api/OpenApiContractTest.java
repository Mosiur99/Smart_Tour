package com.smarttourplanner.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.smarttourplanner.support.TestDatabaseConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fails when the API changes without web/openapi.json, the file the frontend's
 * types are generated from. To refresh it after a deliberate API change, run
 * {@code mvn test -Dtest=OpenApiContractTest -Dopenapi.update=true}
 * and then {@code npm run api:types} in web/.
 */
@SpringBootTest
@Import(TestDatabaseConfig.class)
class OpenApiContractTest {

    private static final Path CONTRACT = Path.of("web/openapi.json");
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    @Autowired
    private WebApplicationContext context;

    @Test
    void savedContractMatchesTheRunningApi() throws Exception {
        String live = liveContract();

        if (Boolean.getBoolean("openapi.update")) {
            Files.writeString(CONTRACT, live);
        }

        assertThat(Files.readString(CONTRACT)).isEqualTo(live);
    }

    @SuppressWarnings("unchecked")
    private String liveContract() throws Exception {
        String body = MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> contract = JSON.readValue(body, Map.class);
        // The server address depends on where the test runs, not on the API.
        contract.remove("servers");
        return JSON.writeValueAsString(contract) + "\n";
    }
}
