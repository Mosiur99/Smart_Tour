package com.smarttourplanner.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldTest {

    private final Field name = Field.text("nameEn", "name_en").notEmpty();
    private final Field price = Field.integer("priceMin", "price_min", 1, 1_000_000);
    private final Field lat = Field.decimal("lat", "lat", "20", "27");
    private final Field months = Field.numbers("months", "months", 1, 12).notEmpty();
    private final Field tags = Field.tags("tags", "tags");

    @Test
    void trimsTextAndTreatsBlankAsEmpty() {
        assertThat(name.convert("  Test Lake ")).isEqualTo("Test Lake");
        assertThat(Field.text("nameBn", "name_bn").convert("   ")).isNull();
        assertProblem(() -> name.convert("  "), "nameEn", "This can't be empty.");
    }

    @Test
    void limitsTextLength() {
        assertProblem(() -> name.convert("x".repeat(201)), "nameEn", "Keep this under 200 characters.");
        assertThat(Field.longText("note", "note").convert("x".repeat(2_000))).isNotNull();
    }

    @Test
    void acceptsWholeNumbersInRangeOnly() {
        assertThat(price.convert(2500)).isEqualTo(2500);
        assertThat(price.convert(2500.0)).isEqualTo(2500);
        assertProblem(() -> price.convert(0), "priceMin", "Use a number from 1 to 1000000.");
        assertProblem(() -> price.convert(12.5), "priceMin", "Use a whole number.");
        assertProblem(() -> price.convert("2500"), "priceMin", "Use a number.");
    }

    @Test
    void keepsDecimalsExact() {
        assertThat(lat.convert(24.89123)).isEqualTo(new BigDecimal("24.89123"));
        assertProblem(() -> lat.convert(30), "lat", "Use a number from 20 to 27.");
    }

    @Test
    void acceptsOnlyHttpsAddresses() {
        Field source = Field.url("sourceUrl", "source_url");
        assertThat(source.convert("https://example.com/fares")).isEqualTo("https://example.com/fares");
        for (String bad : List.of("http://example.com", "javascript:alert(1)", "example.com", "https://")) {
            assertProblem(() -> source.convert(bad), "sourceUrl", "Use a full https:// address.");
        }
    }

    @Test
    void refusesCheckedDatesInTheFuture() {
        Field checked = Field.date("checkedAt", "checked_at");
        assertThat(checked.convert("2026-01-15")).isEqualTo(LocalDate.of(2026, 1, 15));
        assertProblem(() -> checked.convert(LocalDate.now().plusDays(1).toString()), "checkedAt",
                "A checked date can't be in the future.");
        assertProblem(() -> checked.convert("15/01/2026"), "checkedAt", "Use a date like 2026-10-31.");
    }

    @Test
    void acceptsOnlyListedChoices() {
        Field tier = Field.choice("tier", "tier", "hotel_tier", List.of("BUDGET", "MID", "PREMIUM"));
        assertThat(tier.convert("MID")).isEqualTo("MID");
        assertProblem(() -> tier.convert("LUXURY"), "tier", "Choose one of: BUDGET, MID, PREMIUM.");
    }

    @Test
    void acceptsOnlySlugsForHubs() {
        Field hub = Field.hub("fromHub", "from_hub_id");
        assertThat(hub.convert("coxs-bazar")).isEqualTo("coxs-bazar");
        assertProblem(() -> hub.convert("x' OR '1'='1"), "fromHub", "Choose a place from the list.");
    }

    @Test
    void sortsNumberListsAndDropsRepeats() {
        assertThat(months.convert(List.of(12, 1, 2, 1))).isEqualTo(List.of(1, 2, 12));
        assertProblem(() -> months.convert(List.of()), "months", "This can't be empty.");
        assertProblem(() -> months.convert(List.of(13)), "months", "Use a number from 1 to 12.");
    }

    @Test
    void cleansTagsAndKeepsAnEmptyListForNothing() {
        assertThat(tags.convert(List.of(" tea ", "tea", "", "hills, rivers"))).isEqualTo(List.of("tea", "hills, rivers"));
        assertThat(tags.convert(null)).isEqualTo(List.of());
        String[] tooMany = new String[21];
        Arrays.fill(tooMany, "x");
        assertProblem(() -> tags.convert(List.of(tooMany)), "tags", "Use at most 20 tags.");
    }

    @Test
    void buildsSqlOnlyFromItsOwnNames() {
        assertThat(Field.hub("fromHub", "from_hub_id").selectSql())
                .isEqualTo("(SELECT slug FROM hub WHERE id = t.from_hub_id) AS \"fromHub\"");
        assertThat(Field.choice("tier", "tier", "hotel_tier", List.of("MID")).valueSql("v0"))
                .isEqualTo("CAST(:v0 AS hotel_tier)");
        assertThat(months.valueSql("v1")).contains("CAST(:v1 AS jsonb)").contains("::smallint");
    }

    @Test
    void everyKindHasUniqueFieldNames() {
        for (Editable kind : Editable.values()) {
            List<String> names = kind.fields.stream().map(Field::name).toList();
            assertThat(names).doesNotHaveDuplicates();
        }
    }

    private static void assertProblem(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String field,
            String message) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(AdminException.class, e -> assertThat(e.fields()).isEqualTo(Map.of(field, message)));
    }
}
