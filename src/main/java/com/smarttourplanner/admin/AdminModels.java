package com.smarttourplanner.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** What the admin API sends and receives. */
public final class AdminModels {

    private AdminModels() {
    }

    /** Editors prepare records. Only publishers change what visitors see. */
    public enum Role { EDITOR, PUBLISHER }

    public enum FieldType { TEXT, LONG_TEXT, URL, INTEGER, DECIMAL, DATE, CHOICE, HUB, NUMBERS, TAGS }

    /** Statuses an admin can set. IMPORTED only marks data that came from the old site. */
    public enum TargetStatus { PUBLISHED, DRAFT, ARCHIVED }

    public record AdminUser(String email, Role role) {
    }

    public record FieldSpec(
            String name, FieldType type, List<String> options, @Nullable BigDecimal min, @Nullable BigDecimal max,
            boolean required, boolean createOnly) {
    }

    public record EntitySpec(String entity, List<FieldSpec> fields) {
    }

    /** One record, with its field values keyed by field name. */
    @Schema(name = "AdminRecord")
    public record RecordView(
            String entity, long id, int revision, String status, String label, @Nullable String destinationSlug,
            Map<String, @Nullable Object> values) {
    }

    public record DestinationOverview(
            String slug, String nameEn, @Nullable String nameBn, String status, int records, int published,
            int stale) {
    }

    /** A destination with everything that belongs to it and the journeys to and from its arrival hub. */
    public record Workspace(RecordView destination, List<RecordView> places, List<RecordView> hotels,
            List<RecordView> legs) {
    }

    /** A published record whose price or details were last checked too long ago. */
    public record StaleRecord(
            String entity, long id, String label, @Nullable String destinationSlug, LocalDate checkedAt) {
    }

    public record AuditEntry(
            long id, String actor, String entity, long entityId, String action,
            @Nullable Map<String, @Nullable Object> before, @Nullable Map<String, @Nullable Object> after,
            Instant at) {
    }

    /** Changes only the fields sent. The revision must match the one the screen was opened with. */
    public record SaveRequest(@NotNull @PositiveOrZero Integer revision, @NotNull Map<String, @Nullable Object> values) {
    }

    /** A new record always starts as a draft. Places and hotels name their destination. */
    public record CreateRequest(
            @Nullable @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") String destinationSlug,
            @NotNull Map<String, @Nullable Object> values) {
    }

    public record StatusRequest(@NotNull @PositiveOrZero Integer revision, @NotNull TargetStatus status) {
    }
}
