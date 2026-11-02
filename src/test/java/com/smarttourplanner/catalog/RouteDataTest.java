package com.smarttourplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks every record in data/routes before it reaches the database. These rules catch typing
 * mistakes; a person still confirms each fact before publishing it.
 */
class RouteDataTest {

    private static final List<String> ROUTES = List.of("coxs-bazar", "saint-martin", "sylhet", "bandarban", "sajek");
    private static final Pattern BANGLA = Pattern.compile("[\\u0980-\\u09FF]");
    private static final double MAX_KM_FROM_DESTINATION = 60;
    private static final Set<String> KINDS = Set.of("SPOT", "HERITAGE", "MARKET", "FOOD", "CRAFT");
    private static final Set<String> TIERS = Set.of("BUDGET", "MID", "PREMIUM");
    private static final Set<String> MODES = Set.of("BUS", "TRAIN", "LAUNCH", "SHIP", "FLIGHT", "JEEP", "BOAT");
    private static final String ORIGIN = "dhaka";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    static Stream<String> routes() {
        return ROUTES.stream();
    }

    @ParameterizedTest
    @MethodSource("routes")
    void destinationIsComplete(String route) throws IOException {
        JsonNode destination = read(route).get("destination");

        assertThat(destination.get("slug").asString()).isEqualTo(route);
        assertBangla(destination.get("nameBn"), route);
        assertInBangladesh(destination, route);
        assertThat(destination.get("arrivalHub").asString()).isNotBlank();
        assertThat(destination.get("minDays").asInt()).isBetween(1, 14);
        assertThat(destination.get("roadFactor").asDouble()).isBetween(1.0, 3.0);
        assertThat(numbers(destination, "bestMonths")).isNotEmpty().allMatch(month -> month >= 1 && month <= 12);
        assertThat(destination.get("foodDaily").asInt()).isPositive();
        assertThat(destination.get("localTransportDaily").asInt()).isPositive();
    }

    @ParameterizedTest
    @MethodSource("routes")
    void placesAreNamedInBothLanguagesAndNearTheDestination(String route) throws IOException {
        JsonNode data = read(route);
        JsonNode destination = data.get("destination");
        Set<String> keys = new HashSet<>();

        for (JsonNode place : data.get("places")) {
            String name = place.get("nameEn").asString();
            assertThat(KINDS).as(name).contains(place.get("kind").asString());
            assertThat(keys.add(place.get("kind").asString() + ":" + name)).as("%s is listed twice", name).isTrue();
            assertBangla(place.get("nameBn"), name);
            if (place.has("lat")) {
                assertNear(place, destination, name);
            }
            boolean visited = Set.of("SPOT", "HERITAGE").contains(place.get("kind").asString());
            if (visited) {
                assertThat(place.has("lat") && place.has("lng")).as("%s needs a map position", name).isTrue();
                assertThat(place.path("visitMinutes").asInt()).as("%s needs a visit time", name).isBetween(15, 600);
            }
            assertRange(place, "feeMin", "feeMax", name);
        }
        assertThat(data.get("places")).filteredOn(place -> place.get("kind").asString().equals("SPOT"))
                .as("%s needs at least three sights", route)
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("routes")
    void hotelPricesComeWithASource(String route) throws IOException {
        JsonNode data = read(route);
        for (JsonNode hotel : data.get("hotels")) {
            String name = hotel.get("nameEn").asString();
            assertBangla(hotel.get("nameBn"), name);
            assertNear(hotel, data.get("destination"), name);
            if (hotel.has("tier")) {
                assertThat(TIERS).as(name).contains(hotel.get("tier").asString());
            }
            assertThat(hotel.has("priceMin")).as("%s needs both prices or neither", name).isEqualTo(hotel.has("priceMax"));
            if (hotel.has("priceMin")) {
                assertRange(hotel, "priceMin", "priceMax", name);
                assertSecureAddress(hotel.get("sourceUrl"), name);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("routes")
    void faresComeWithASourceAndJourneysAreSensible(String route) throws IOException {
        for (JsonNode leg : read(route).get("legs")) {
            String name = "%s to %s by %s".formatted(leg.get("from").asString(), leg.get("to").asString(),
                    leg.get("mode").asString());
            assertThat(MODES).as(name).contains(leg.get("mode").asString());
            assertThat(leg.get("from").asString()).as(name).isNotEqualTo(leg.get("to").asString());
            assertThat(leg.get("durationMinutes").asInt()).as(name).isBetween(10, 1_440);
            assertThat(numbers(leg, "months")).as(name).allMatch(month -> month >= 1 && month <= 12);
            assertThat(leg.has("fareMin")).as("%s needs both fares or neither", name).isEqualTo(leg.has("fareMax"));
            if (leg.has("fareMin")) {
                assertRange(leg, "fareMin", "fareMax", name);
                assertSecureAddress(leg.get("sourceUrl"), name);
            }
        }
    }

    /** Every route must be reachable from Dhaka and back, using this route's journeys and earlier routes'. */
    @ParameterizedTest
    @MethodSource("routes")
    void connectsToDhakaBothWays(String route) throws IOException {
        List<JsonNode> legs = new ArrayList<>();
        for (String other : ROUTES) {
            read(other).get("legs").forEach(legs::add);
        }
        String arrival = read(route).get("destination").get("arrivalHub").asString();

        assertThat(reachable(legs, ORIGIN)).as("Dhaka to %s", route).contains(arrival);
        assertThat(reachable(legs, arrival)).as("%s to Dhaka", route).contains(ORIGIN);
    }

    private static Set<String> reachable(List<JsonNode> legs, String from) {
        Set<String> reached = new HashSet<>(Set.of(from));
        for (int round = 0; round < 3; round++) {
            for (JsonNode leg : legs) {
                if (reached.contains(leg.get("from").asString())) {
                    reached.add(leg.get("to").asString());
                }
            }
        }
        return reached;
    }

    private static void assertBangla(JsonNode name, String what) {
        assertThat(name).as("%s needs a Bangla name", what).isNotNull();
        assertThat(BANGLA.matcher(name.asString()).find()).as("%s's Bangla name has no Bangla letters", what).isTrue();
    }

    private static void assertInBangladesh(JsonNode node, String what) {
        assertThat(node.get("lat").asDouble()).as(what).isBetween(20.0, 27.0);
        assertThat(node.get("lng").asDouble()).as(what).isBetween(88.0, 93.0);
    }

    private static void assertNear(JsonNode node, JsonNode destination, String what) {
        assertInBangladesh(node, what);
        double km = kilometres(node.get("lat").asDouble(), node.get("lng").asDouble(),
                destination.get("lat").asDouble(), destination.get("lng").asDouble());
        assertThat(km).as("%s is %.0f km from the destination; check its map position", what, km)
                .isLessThan(MAX_KM_FROM_DESTINATION);
    }

    private static void assertRange(JsonNode node, String low, String high, String what) {
        if (node.has(low) && node.has(high)) {
            assertThat(node.get(low).asInt()).as(what).isPositive().isLessThanOrEqualTo(node.get(high).asInt());
        }
    }

    private static void assertSecureAddress(JsonNode address, String what) {
        assertThat(address).as("%s needs a source for its price", what).isNotNull();
        URI uri = URI.create(address.asString());
        assertThat(uri.getScheme()).as(what).isEqualTo("https");
        assertThat(uri.getHost()).as(what).isNotBlank();
    }

    private static List<Integer> numbers(JsonNode node, String field) {
        List<Integer> numbers = new ArrayList<>();
        node.path(field).forEach(item -> numbers.add(item.asInt()));
        return numbers;
    }

    private static double kilometres(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(a));
    }

    private static JsonNode read(String route) throws IOException {
        try (InputStream input = new ClassPathResource("data/routes/" + route + ".json").getInputStream()) {
            return JSON.readTree(input);
        }
    }
}
