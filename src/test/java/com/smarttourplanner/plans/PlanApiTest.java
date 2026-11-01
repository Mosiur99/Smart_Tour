package com.smarttourplanner.plans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smarttourplanner.support.TestDatabaseConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "smarttour.catalog.version-check-interval=0s")
@Import(TestDatabaseConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanApiTest {

    private static final String PLANS = "/api/v1/plans";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcClient jdbc;

    private MockMvc mvc;

    @BeforeAll
    void publishTestTrip() {
        jdbc.sql("""
                UPDATE destination SET status = 'PUBLISHED', lat = 24.89, lng = 91.87, min_days = 2,
                    best_months = '{10,11,12,1,2}'
                WHERE slug = 'sylhet'
                """).update();
        long destination = jdbc.sql("SELECT id FROM destination WHERE slug = 'sylhet'").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO poi (destination_id, kind, name_en, name_bn, lat, lng, priority, visit_minutes, fee_min,
                    fee_max, checked_at, status)
                VALUES (:destination, 'SPOT', 'Test Lake', 'পরীক্ষা হ্রদ', 24.90, 91.88, 1, 90, 50, 100, current_date, 'PUBLISHED')
                """).param("destination", destination).update();
        jdbc.sql("""
                INSERT INTO hotel (destination_id, name_en, tier, lat, lng, price_min, price_max, source_url,
                    checked_at, status)
                VALUES (:destination, 'Test Hotel', 'MID', 24.90, 91.88, 2000, 3000, 'https://example.com', current_date, 'PUBLISHED')
                """).param("destination", destination).update();
        jdbc.sql("""
                UPDATE transport_leg SET status = 'PUBLISHED', duration_min = 420, checked_at = current_date,
                    source_url = 'https://example.com'
                WHERE mode = 'BUS'
                  AND (from_hub_id = (SELECT id FROM hub WHERE slug = 'sylhet')
                       OR to_hub_id = (SELECT id FROM hub WHERE slug = 'sylhet'))
                """).update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createsAPlanWithALink() throws Exception {
        create(body("dhaka", "sylhet", 3, 2, 12))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(PLANS + "/")))
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.plan.destinationNameEn", is("Sylhet")))
                .andExpect(jsonPath("$.plan.dayPlans", hasSize(3)))
                .andExpect(jsonPath("$.plan.dayPlans[0].stops[0].stop.nameEn", is("Test Lake")))
                .andExpect(jsonPath("$.plan.hotel.nameEn", is("Test Hotel")))
                .andExpect(jsonPath("$.plan.budget.low", greaterThan(0)));
    }

    @Test
    void returnsTheSamePlanForTheSameRequest() throws Exception {
        String first = publicId(create(body("dhaka", "sylhet", 4, 2, 12)));
        ResultActions repeat = create(body("dhaka", "sylhet", 4, 2, 12)).andExpect(status().isOk());

        assertThat(publicId(repeat)).isEqualTo(first);
    }

    @Test
    void storesOnePlanWhenIdenticalRequestsArriveTogether() throws Exception {
        String request = body("dhaka", "sylhet", 5, 2, 12);
        List<Callable<String>> calls = new ArrayList<>();
        for (int count = 0; count < 12; count++) {
            calls.add(() -> publicId(create(request)));
        }

        List<String> ids = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(12)) {
            for (Future<String> result : pool.invokeAll(calls)) {
                ids.add(result.get());
            }
        }

        assertThat(ids).containsOnly(ids.get(0));
    }

    @Test
    void readsASavedPlanThroughItsLink() throws Exception {
        String publicId = publicId(create(body("dhaka", "sylhet", 2, 2, 12)));

        mvc.perform(get(PLANS + "/" + publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId", is(publicId)))
                .andExpect(jsonPath("$.request.originHub", is("dhaka")))
                .andExpect(header().string("Cache-Control", containsString("s-maxage=86400")));
    }

    @Test
    void keepsASavedPlanUnchangedAfterDataChanges() throws Exception {
        String publicId = publicId(create(body("dhaka", "sylhet", 6, 1, 12)));
        jdbc.sql("UPDATE hotel SET price_min = 9000, price_max = 9500 WHERE name_en = 'Test Hotel'").update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();

        mvc.perform(get(PLANS + "/" + publicId)).andExpect(jsonPath("$.plan.hotel.priceMin", is(2000)));

        String newId = publicId(create(body("dhaka", "sylhet", 6, 1, 12)));
        assertThat(newId).isNotEqualTo(publicId);
        mvc.perform(get(PLANS + "/" + newId)).andExpect(jsonPath("$.plan.hotel.priceMin", is(9000)));

        jdbc.sql("UPDATE hotel SET price_min = 2000, price_max = 3000 WHERE name_en = 'Test Hotel'").update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();
    }

    @Test
    void warnsAboutOffSeasonTravel() throws Exception {
        create(body("dhaka", "sylhet", 3, 2, 7)).andExpect(jsonPath("$.plan.notices[0]", is("OFF_SEASON")));
    }

    @Test
    void doesNotPlanForDestinationsThatArentPublished() throws Exception {
        expectProblem(create(body("dhaka", "rajshahi", 3, 2, 12)), 404);
    }

    @Test
    void rejectsAnUnknownStartingPoint() throws Exception {
        expectProblem(create(body("atlantis", "sylhet", 3, 2, 12)), 404);
    }

    @Test
    void explainsWhenNoRouteExists() throws Exception {
        expectProblem(create(body("rangpur", "sylhet", 3, 2, 12)), 422)
                .andExpect(jsonPath("$.detail", containsString("route")));
    }

    @Test
    void rejectsInvalidTrips() throws Exception {
        expectProblem(create(body("dhaka", "sylhet", 0, 2, 12)), 400);
        expectProblem(create(body("dhaka", "sylhet", 15, 2, 12)), 400);
        expectProblem(create(body("dhaka", "sylhet", 3, 31, 12)), 400);
        expectProblem(create(body("dhaka", "sylhet", 3, 2, 13)), 400);
        expectProblem(create(body("Bad Slug", "sylhet", 3, 2, 12)), 400);
    }

    @Test
    void rejectsUnknownBudgetLevels() throws Exception {
        expectProblem(create("""
                {"originHub":"dhaka","destination":"sylhet","days":3,"people":2,"budget":"FREE","month":12}
                """), 400);
    }

    @Test
    void rejectsBrokenJson() throws Exception {
        expectProblem(create("{not json"), 400);
    }

    @Test
    void unknownPlanReturnsAProblem() throws Exception {
        expectProblem(mvc.perform(get(PLANS + "/abcdefghjkmn")), 404);
    }

    @Test
    void rejectsAMalformedPlanId() throws Exception {
        expectProblem(mvc.perform(get(PLANS + "/too-short")), 400);
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post(PLANS).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String publicId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.publicId");
    }

    private ResultActions expectProblem(ResultActions result, int status) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").exists());
    }

    private static String body(String origin, String destination, int days, int people, int month) {
        return """
                {"originHub":"%s","destination":"%s","days":%d,"people":%d,"budget":"MID","month":%d,"interests":["NATURE"]}
                """.formatted(origin, destination, days, people, month);
    }
}
