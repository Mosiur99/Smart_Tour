package com.smarttourplanner.plans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarttourplanner.support.TestDatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The researched route data on real PostgreSQL. It must arrive unchecked, and once a person has
 * checked it, each of the five routes must produce a plan from Dhaka.
 */
// Its own database: other test classes share one and publish records in it.
@SpringBootTest(properties = {"smarttour.catalog.version-check-interval=0s", "smarttour.test.database=route-data"})
@Import(TestDatabaseConfig.class)
// The first two tests look at the data as it arrives; the rest publish it, so they come after.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RouteDataApiTest {

    private static final String ROUTES = "('coxs-bazar', 'saint-martin', 'sylhet', 'bandarban', 'sajek')";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcClient jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @Order(1)
    void arrivesUncheckedSoNothingReachesVisitorsByItself() {
        assertThat(count("SELECT count(*) FROM poi WHERE status = 'PUBLISHED' OR checked_at IS NOT NULL")).isZero();
        assertThat(count("SELECT count(*) FROM hotel WHERE status = 'PUBLISHED' OR checked_at IS NOT NULL")).isZero();
        assertThat(count("SELECT count(*) FROM transport_leg WHERE status = 'PUBLISHED' OR checked_at IS NOT NULL"))
                .isZero();
        assertThat(count("SELECT count(*) FROM destination WHERE slug IN ('sajek', 'saint-martin') AND status = 'DRAFT'"))
                .isEqualTo(2);
    }

    @Test
    @Order(2)
    void enrichesImportedRecordsInsteadOfRepeatingThem() {
        assertThat(count("""
                SELECT count(*) FROM poi JOIN destination d ON d.id = poi.destination_id
                WHERE d.slug = 'coxs-bazar' AND poi.name_en = 'Inani Beach'
                """)).isEqualTo(1);
        assertThat(jdbc.sql("""
                SELECT poi.status::text FROM poi JOIN destination d ON d.id = poi.destination_id
                WHERE d.slug = 'coxs-bazar' AND poi.name_en = 'Teknaf and Saint Martin route gateway'
                """).query(String.class).single()).isEqualTo("ARCHIVED");
    }

    @Order(3)
    @ParameterizedTest
    @CsvSource({
        "coxs-bazar, 12, TRAIN",
        "saint-martin, 12, SHIP",
        "sylhet, 10, BUS",
        "bandarban, 11, BUS",
        "sajek, 10, JEEP"
    })
    void eachRoutePlansFromDhakaOnceChecked(String route, int month, String modeOnTheWay) throws Exception {
        checkAndPublishEverythingComplete();

        String request = """
                {"originHub": "dhaka", "destination": "%s", "days": 3, "people": 2, "budget": "MID",
                 "month": %d, "interests": []}
                """.formatted(route, month);
        mvc.perform(post("/api/v1/plans").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plan.destination", is(route)))
                .andExpect(jsonPath("$.plan.outbound.legs[*].mode", hasItem(modeOnTheWay)))
                .andExpect(jsonPath("$.plan.inbound.legs[-1].toHub", is("dhaka")))
                .andExpect(jsonPath("$.plan.dayPlans[0].stops.length()", org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @Order(4)
    void saintMartinHasNoRouteOutsideTheOvernightMonths() throws Exception {
        checkAndPublishEverythingComplete();

        String november = """
                {"originHub": "dhaka", "destination": "saint-martin", "days": 3, "people": 2, "budget": "MID",
                 "month": 11, "interests": []}
                """;
        mvc.perform(post("/api/v1/plans").contentType(MediaType.APPLICATION_JSON).content(november))
                .andExpect(status().isUnprocessableContent());
    }

    /** What a person does in the admin panel after checking: set today's date and publish what's complete. */
    private void checkAndPublishEverythingComplete() {
        jdbc.sql("UPDATE destination SET status = 'PUBLISHED' WHERE slug IN " + ROUTES).update();
        jdbc.sql("""
                UPDATE poi SET checked_at = current_date, status = 'PUBLISHED'
                WHERE status = 'DRAFT' AND name_bn IS NOT NULL
                  AND destination_id IN (SELECT id FROM destination WHERE slug IN %s)
                """.formatted(ROUTES)).update();
        jdbc.sql("""
                UPDATE hotel SET checked_at = current_date, status = 'PUBLISHED'
                WHERE status = 'DRAFT' AND tier IS NOT NULL AND price_min IS NOT NULL
                  AND destination_id IN (SELECT id FROM destination WHERE slug IN %s)
                """.formatted(ROUTES)).update();
        jdbc.sql("""
                UPDATE transport_leg SET checked_at = current_date, status = 'PUBLISHED'
                WHERE status = 'DRAFT' AND duration_min IS NOT NULL
                """).update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}
