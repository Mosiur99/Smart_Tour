package com.smarttourplanner.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smarttourplanner.admin.AdminService.CatalogChanged;
import com.smarttourplanner.support.TestDatabaseConfig;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The admin API on real PostgreSQL. The version check is slow here, to prove a publish refreshes at once. */
@SpringBootTest(properties = {
    "smarttour.catalog.version-check-interval=1h",
    "smarttour.admin.publishers=owner@example.com",
    "smarttour.admin.editors=helper@example.com"
})
@Import({TestDatabaseConfig.class, AdminApiTest.TestLogin.class})
@RecordApplicationEvents
class AdminApiTest {

    private static final String OWNER = "owner@example.com";
    private static final String HELPER = "helper@example.com";
    private static final String ADMIN = "/api/v1/admin";
    private static final AccessTokens TOKENS = new AccessTokens();

    /** Trusts tokens signed with the test key, in place of Cloudflare's keys. */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestLogin {
        @Bean
        AccessTokenVerifier accessTokenVerifier() {
            return TOKENS.verifier();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEvents events;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Who may do what

    @Test
    void refusesRequestsWithoutALogin() throws Exception {
        mvc.perform(get(ADMIN + "/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Sign in through Cloudflare Access first.")))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void refusesTokensSignedByAnyoneButCloudflare() throws Exception {
        String forged = AccessTokens.sign(TOKENS.claims(OWNER).build(), AccessTokens.generate());
        mvc.perform(get(ADMIN + "/me").header(AccessTokenVerifier.HEADER, forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesSignedInPeopleWhoAreNotAdmins() throws Exception {
        mvc.perform(as("stranger@example.com", get(ADMIN + "/destinations")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", is("This account isn't an admin.")));
    }

    @Test
    void tellsThePanelWhoIsSignedIn() throws Exception {
        mvc.perform(as("Helper@Example.com", get(ADMIN + "/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(HELPER)))
                .andExpect(jsonPath("$.role", is("EDITOR")));
    }

    @Test
    void editorsCannotPublish() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());

        mvc.perform(as(HELPER, post(ADMIN + "/hotels/" + hotel + "/status")).content(statusBody(0, "PUBLISHED")))
                .andExpect(status().isForbidden());

        assertThat(statusOf(hotel)).isEqualTo("DRAFT");
    }

    @Test
    void editorsCannotChangeWhatVisitorsSee() throws Exception {
        long hotel = completeHotel("PUBLISHED", LocalDate.now());

        mvc.perform(as(HELPER, put(ADMIN + "/hotels/" + hotel)).content(save(0, "{\"priceMin\": 1}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", is("Only a publisher can change what visitors see.")));
    }

    @Test
    void editorsCanPrepareDrafts() throws Exception {
        String name = unique("Draft Inn");
        String body = """
                {"destinationSlug": "sylhet", "values": {"nameEn": "%s", "roomCapacity": 2}}
                """.formatted(name);
        String created = mvc.perform(as(HELPER, post(ADMIN + "/hotels")).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.destinationSlug", is("sylhet")))
                .andReturn().getResponse().getContentAsString();
        long id = com.jayway.jsonpath.JsonPath.<Number>read(created, "$.id").longValue();

        mvc.perform(as(HELPER, put(ADMIN + "/hotels/" + id)).content(save(0, "{\"nameBn\": \"খসড়া\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.nameBn", is("খসড়া")))
                .andExpect(jsonPath("$.revision", is(1)));
    }

    @Test
    void refusesChangesSentFromOtherSites() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());

        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel)).header("Sec-Fetch-Site", "cross-site")
                        .content(save(0, "{\"priceMin\": 1}")))
                .andExpect(status().isForbidden());
    }

    // Editing and publishing

    @Test
    void refusesToPublishIncompleteRecords() throws Exception {
        long hotel = jdbc.sql("""
                INSERT INTO hotel (destination_id, name_en) VALUES ((SELECT id FROM destination WHERE slug = 'sylhet'), :name)
                RETURNING id
                """).param("name", unique("Half Done")).query(Long.class).single();

        mvc.perform(as(OWNER, post(ADMIN + "/hotels/" + hotel + "/status")).content(statusBody(0, "PUBLISHED")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail",
                        is("Add the tier, map position, prices, source and checked date before publishing.")));

        assertThat(statusOf(hotel)).isEqualTo("DRAFT");
        assertThat(auditActions(hotel)).isEmpty();
    }

    @Test
    void publishesCompleteRecordsAndRecordsWhoDidIt() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());
        int version = catalogVersion();

        mvc.perform(as(OWNER, post(ADMIN + "/hotels/" + hotel + "/status")).content(statusBody(0, "PUBLISHED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.revision", is(1)));

        assertThat(catalogVersion()).isEqualTo(version + 1);
        mvc.perform(as(HELPER, get(ADMIN + "/history?kind=hotels&id=" + hotel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action", is("PUBLISH")))
                .andExpect(jsonPath("$[0].actor", is(OWNER)))
                .andExpect(jsonPath("$[0].before.status", is("DRAFT")))
                .andExpect(jsonPath("$[0].after.status", is("PUBLISHED")))
                .andExpect(jsonPath("$[0].after.priceMin", is(1500)));
    }

    @Test
    void unpublishingHidesARecordAgain() throws Exception {
        long hotel = completeHotel("PUBLISHED", LocalDate.now());

        mvc.perform(as(OWNER, post(ADMIN + "/hotels/" + hotel + "/status")).content(statusBody(0, "DRAFT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        assertThat(auditActions(hotel)).containsExactly("UNPUBLISH");
    }

    @Test
    void refusesSavesFromAnOutOfDateScreen() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());
        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel)).content(save(0, "{\"priceMax\": 3000}")))
                .andExpect(status().isOk());

        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel)).content(save(0, "{\"priceMax\": 4000}")))
                .andExpect(status().isConflict());

        assertThat(jdbc.sql("SELECT price_max FROM hotel WHERE id = :id").param("id", hotel).query(Integer.class).single())
                .isEqualTo(3000);
    }

    @Test
    void listsEveryProblemWithASaveAtOnce() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());

        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel))
                        .content(save(0, "{\"priceMin\": 0, \"website\": \"http://example.com\", \"stars\": 5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.priceMin", is("Use a number from 1 to 1000000.")))
                .andExpect(jsonPath("$.fields.website", is("Use a full https:// address.")))
                .andExpect(jsonPath("$.fields.stars", is("This field doesn't exist.")));
    }

    @Test
    void explainsRulesThatSpanTwoFields() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());

        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel)).content(save(0, "{\"priceMax\": 100}")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", is("Give both prices, with the highest not lower than the lowest.")));
    }

    @Test
    void explainsRepeatedNames() throws Exception {
        String body = """
                {"destinationSlug": "sylhet", "values": {"nameEn": "%s", "roomCapacity": 2}}
                """.formatted(unique("Twin Inn"));
        mvc.perform(as(OWNER, post(ADMIN + "/hotels")).content(body)).andExpect(status().isCreated());

        mvc.perform(as(OWNER, post(ADMIN + "/hotels")).content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", is("Another record here already has this name.")));
    }

    @Test
    void keepsTheEndsOfAJourneyFixed() throws Exception {
        long leg = jdbc.sql("SELECT min(id) FROM transport_leg").query(Long.class).single();

        mvc.perform(as(OWNER, put(ADMIN + "/legs/" + leg)).content(save(0, "{\"fromHub\": \"dhaka\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fromHub", is("This can't change after the record is added.")));
    }

    @Test
    void addsJourneysOnlyBetweenKnownHubs() throws Exception {
        String body = """
                {"values": {"fromHub": "dhaka", "toHub": "atlantis", "mode": "BUS", "serviceName": "Test",
                            "months": [1, 2]}}
                """;
        mvc.perform(as(OWNER, post(ADMIN + "/legs")).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.toHub", is("Choose a place from the list.")));
    }

    @Test
    void asksForEveryRequiredFieldOfANewRecord() throws Exception {
        mvc.perform(as(OWNER, post(ADMIN + "/places")).content("{\"destinationSlug\": \"sylhet\", \"values\": {}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.kind", is("This can't be empty.")))
                .andExpect(jsonPath("$.fields.nameEn", is("This can't be empty.")));
    }

    @Test
    void doesNotAddDestinations() throws Exception {
        mvc.perform(as(OWNER, post(ADMIN + "/destinations")).content("{\"values\": {\"nameEn\": \"New\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesBodiesThatAreNotJson() throws Exception {
        mvc.perform(as(OWNER, post(ADMIN + "/hotels")).contentType(MediaType.TEXT_PLAIN).content("nameEn=x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // Caches

    @Test
    void aPriceChangeShowsOnThePublicApiAtOnce() throws Exception {
        long hotel = completeHotel("PUBLISHED", LocalDate.now());
        String priceOfHotel = "$.hotels[?(@.nameEn == '%s')].priceMin".formatted(nameOf(hotel));
        mvc.perform(get("/api/v1/destinations/sylhet")).andExpect(jsonPath(priceOfHotel, hasItem(1500)));

        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel)).content(save(0, "{\"priceMin\": 1800}")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/destinations/sylhet")).andExpect(jsonPath(priceOfHotel, hasItem(1800)));
        assertThat(events.stream(CatalogChanged.class)).hasSize(1);
    }

    @Test
    void draftChangesLeaveCachesAlone() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());
        int version = catalogVersion();

        mvc.perform(as(OWNER, put(ADMIN + "/hotels/" + hotel)).content(save(0, "{\"priceMin\": 1200}")))
                .andExpect(status().isOk());

        assertThat(catalogVersion()).isEqualTo(version);
        assertThat(events.stream(CatalogChanged.class)).isEmpty();
    }

    // Lists

    @Test
    void listsRecordsCheckedTooLongAgoOldestFirst() throws Exception {
        long old = completeHotel("PUBLISHED", LocalDate.now().minusDays(120));
        long older = completeHotel("PUBLISHED", LocalDate.now().minusDays(400));
        long fresh = completeHotel("PUBLISHED", LocalDate.now().minusDays(10));

        String body = mvc.perform(as(HELPER, get(ADMIN + "/stale")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.util.List<Number> ids = com.jayway.jsonpath.JsonPath.read(body, "$[?(@.entity == 'hotels')].id");
        assertThat(ids.stream().map(Number::longValue).toList())
                .containsSubsequence(older, old)
                .doesNotContain(fresh);
    }

    @Test
    void showsADestinationWithEverythingInIt() throws Exception {
        long hotel = completeHotel("DRAFT", LocalDate.now());

        mvc.perform(as(HELPER, get(ADMIN + "/destinations/sylhet")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination.values.nameEn", is("Sylhet")))
                .andExpect(jsonPath("$.hotels[*].id", hasItem((int) hotel)))
                .andExpect(jsonPath("$.places.length()", not(0)));
        mvc.perform(as(HELPER, get(ADMIN + "/destinations/atlantis"))).andExpect(status().isNotFound());
    }

    @Test
    void countsWhatEachDestinationNeeds() throws Exception {
        completeHotel("PUBLISHED", LocalDate.now().minusDays(200));

        mvc.perform(as(HELPER, get(ADMIN + "/destinations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(66)))
                .andExpect(jsonPath("$[?(@.slug == 'sylhet')].stale", hasItem(org.hamcrest.Matchers.greaterThan(0))));
    }

    @Test
    void describesTheFormsForEachKindOfRecord() throws Exception {
        mvc.perform(as(HELPER, get(ADMIN + "/schema")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].entity", org.hamcrest.Matchers.contains("destinations", "places", "hotels", "legs")))
                .andExpect(jsonPath("$[?(@.entity == 'hotels')].fields[?(@.name == 'tier')].options[0]", hasItem("BUDGET")));
    }

    private static MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder request) {
        return request.header(AccessTokenVerifier.HEADER, TOKENS.forEmail(email))
                .contentType(MediaType.APPLICATION_JSON);
    }

    private long completeHotel(String status, LocalDate checkedAt) {
        return jdbc.sql("""
                INSERT INTO hotel (destination_id, name_en, name_bn, tier, lat, lng, price_min, price_max, source_url,
                                   checked_at, status)
                VALUES ((SELECT id FROM destination WHERE slug = 'sylhet'), :name, 'পরীক্ষা', 'MID', 24.9, 91.87,
                        1500, 2500, 'https://example.com/rates', :checked, CAST(:status AS record_status))
                RETURNING id
                """)
                .param("name", unique("Test Hotel"))
                .param("checked", checkedAt)
                .param("status", status)
                .query(Long.class)
                .single();
    }

    private String nameOf(long hotel) {
        return jdbc.sql("SELECT name_en FROM hotel WHERE id = :id").param("id", hotel).query(String.class).single();
    }

    private String statusOf(long hotel) {
        return jdbc.sql("SELECT status::text FROM hotel WHERE id = :id").param("id", hotel).query(String.class).single();
    }

    private java.util.List<String> auditActions(long hotel) {
        return jdbc.sql("SELECT action FROM audit_log WHERE entity = 'hotels' AND entity_id = :id ORDER BY id")
                .param("id", hotel).query(String.class).list();
    }

    private int catalogVersion() {
        return jdbc.sql("SELECT version FROM catalog_version").query(Integer.class).single();
    }

    private static String save(int revision, String values) {
        return "{\"revision\": %d, \"values\": %s}".formatted(revision, values);
    }

    private static String statusBody(int revision, String status) {
        return "{\"revision\": %d, \"status\": \"%s\"}".formatted(revision, status);
    }

    private static String unique(String name) {
        return name + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
