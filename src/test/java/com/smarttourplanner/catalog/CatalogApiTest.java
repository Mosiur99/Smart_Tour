package com.smarttourplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smarttourplanner.support.TestDatabaseConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class CatalogApiTest {

    private static final String DESTINATIONS = "/api/v1/destinations";

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
    void listsHubsSortedByName() throws Exception {
        mvc.perform(get("/api/v1/hubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(64)))
                .andExpect(jsonPath("$[0].nameEn", is("Bagerhat")))
                .andExpect(jsonPath("$[?(@.slug == 'rajshahi')].nameBn", hasItem("রাজশাহী")))
                .andExpect(header().string("Cache-Control", containsString("s-maxage=3600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    void walksEveryDestinationThroughCursorsWithoutRepeats() throws Exception {
        List<String> slugs = new ArrayList<>();
        String cursor = null;
        do {
            String url = DESTINATIONS + "?limit=25" + (cursor != null ? "&cursor=" + cursor : "");
            String body = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            slugs.addAll(JsonPath.read(body, "$.items[*].slug"));
            cursor = JsonPath.read(body, "$.nextCursor");
        } while (cursor != null);

        assertThat(slugs).hasSize(64).isSorted();
        assertThat(new HashSet<>(slugs)).hasSize(64);
    }

    @Test
    void lastPageHasNoCursor() throws Exception {
        mvc.perform(get(DESTINATIONS + "?limit=50&cursor=rangpur"))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void filtersByDivision() throws Exception {
        mvc.perform(get(DESTINATIONS + "?division=sylhet"))
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[*].divisionSlug", everyItem(is("sylhet"))));
    }

    @Test
    void marksImportedDestinationsAsUnverified() throws Exception {
        mvc.perform(get(DESTINATIONS + "?limit=1")).andExpect(jsonPath("$.items[0].verified", is(false)));
    }

    @Test
    void findsAnExactName() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "Rajshahi"))
                .andExpect(jsonPath("$.items[0].slug", is("rajshahi")))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void findsANameWithASpellingMistake() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "Rajshai")).andExpect(jsonPath("$.items[0].slug", is("rajshahi")));
    }

    @Test
    void findsPartOfAName() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "bandar")).andExpect(jsonPath("$.items[0].slug", is("bandarban")));
    }

    @Test
    void findsABanglaName() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "রাজশাহী")).andExpect(jsonPath("$.items[0].slug", is("rajshahi")));
    }

    @Test
    void findsABanglaNameWithASpellingMistake() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "রাজশাহি")).andExpect(jsonPath("$.items[0].slug", is("rajshahi")));
    }

    @Test
    void combinesSearchWithDivision() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "Rajshahi").param("division", "sylhet"))
                .andExpect(jsonPath("$.items", empty()));
    }

    @Test
    void treatsWildcardCharactersAsPlainText() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "%")).andExpect(jsonPath("$.items", empty()));
    }

    @Test
    void treatsSqlAsPlainText() throws Exception {
        mvc.perform(get(DESTINATIONS).param("q", "'; DROP TABLE destination; --")).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT count(*) FROM destination").query(Integer.class).single()).isGreaterThanOrEqualTo(64);
    }

    @Test
    void returnsTheFullDestination() throws Exception {
        mvc.perform(get(DESTINATIONS + "/rajshahi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.nameBn", is("রাজশাহী")))
                .andExpect(jsonPath("$.summary.divisionNameEn", is("Rajshahi")))
                .andExpect(jsonPath("$.places.spots.length()", greaterThan(0)))
                .andExpect(jsonPath("$.places.heritage.length()", greaterThan(0)))
                .andExpect(jsonPath("$.places.food.length()", greaterThan(0)))
                .andExpect(jsonPath("$.hotels.length()", greaterThan(0)))
                .andExpect(jsonPath("$.transport.arriving[?(@.mode == 'BUS')].fareMax", hasItem(930)))
                .andExpect(jsonPath("$.transport.leaving[*].toHub", everyItem(is("dhaka"))))
                .andExpect(header().string("Cache-Control", containsString("s-maxage=600")));
    }

    @Test
    void returnsUnknownHotelPricesAsNull() throws Exception {
        mvc.perform(get(DESTINATIONS + "/rangamati")).andExpect(jsonPath("$.hotels[0].priceMin", nullValue()));
    }

    @Test
    void unknownDestinationReturnsAProblem() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS + "/atlantis")), 404)
                .andExpect(jsonPath("$.detail", containsString("atlantis")));
    }

    @Test
    void rejectsALimitOfZero() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS + "?limit=0")), 400);
    }

    @Test
    void rejectsALimitAboveTheMaximum() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS + "?limit=51")), 400);
    }

    @Test
    void rejectsALimitThatIsNotANumber() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS + "?limit=many")), 400);
    }

    @Test
    void rejectsAMalformedCursor() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS).param("cursor", "../secret")), 400);
    }

    @Test
    void rejectsASearchThatIsTooLong() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS).param("q", "a".repeat(61))), 400);
    }

    @Test
    void rejectsAMalformedSlug() throws Exception {
        expectProblem(mvc.perform(get(DESTINATIONS + "/Bad_Slug")), 400);
    }

    @Test
    void hidesDraftAndArchivedRecords() throws Exception {
        long districtId = jdbc.sql("SELECT id FROM district WHERE slug = 'dhaka'").query(Long.class).single();
        for (String status : new String[] {"DRAFT", "ARCHIVED"}) {
            jdbc.sql("""
                    INSERT INTO destination (slug, district_id, name_en, status)
                    VALUES (:slug, :district, 'Hidden Place', :status::record_status)
                    """)
                    .param("slug", "hidden-" + status.toLowerCase())
                    .param("district", districtId)
                    .param("status", status)
                    .update();
        }

        mvc.perform(get(DESTINATIONS + "/hidden-draft")).andExpect(status().isNotFound());
        mvc.perform(get(DESTINATIONS + "/hidden-archived")).andExpect(status().isNotFound());
        mvc.perform(get(DESTINATIONS).param("q", "Hidden Place")).andExpect(jsonPath("$.items", empty()));
    }

    @Test
    void showsNewDataOnceTheCatalogVersionGoesUp() throws Exception {
        mvc.perform(get(DESTINATIONS + "/sylhet")).andExpect(jsonPath("$.travelNote", not("Updated note")));

        jdbc.sql("UPDATE destination SET travel_note = 'Updated note' WHERE slug = 'sylhet'").update();
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();

        mvc.perform(get(DESTINATIONS + "/sylhet")).andExpect(jsonPath("$.travelNote", is("Updated note")));
    }

    @Test
    void publishesAnOpenApiDescription() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/hubs']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/destinations']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/destinations/{slug}']").exists());
    }

    private ResultActions expectProblem(ResultActions result, int status) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(status)))
                .andExpect(jsonPath("$.detail").exists());
    }
}
