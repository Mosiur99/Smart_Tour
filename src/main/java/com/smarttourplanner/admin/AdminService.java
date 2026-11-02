package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminModels.AdminUser;
import com.smarttourplanner.admin.AdminModels.AuditEntry;
import com.smarttourplanner.admin.AdminModels.CreateRequest;
import com.smarttourplanner.admin.AdminModels.DestinationOverview;
import com.smarttourplanner.admin.AdminModels.EntitySpec;
import com.smarttourplanner.admin.AdminModels.FieldType;
import com.smarttourplanner.admin.AdminModels.RecordView;
import com.smarttourplanner.admin.AdminModels.Role;
import com.smarttourplanner.admin.AdminModels.SaveRequest;
import com.smarttourplanner.admin.AdminModels.StaleRecord;
import com.smarttourplanner.admin.AdminModels.StatusRequest;
import com.smarttourplanner.admin.AdminModels.Workspace;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edits catalog records. Each change is written to the audit log in the same transaction, and a
 * change to anything visitors can see raises the catalog version, so caches refresh without a deploy.
 */
@Service
class AdminService {

    /** Prices older than this get a warning and show on the checking list. */
    static final int STALE_AFTER_DAYS = 90;

    private static final Set<String> VISIBLE = Set.of("IMPORTED", "PUBLISHED");

    private final AdminRepository repository;
    private final ApplicationEventPublisher events;

    AdminService(AdminRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    List<EntitySpec> schema() {
        return Stream.of(Editable.values())
                .map(kind -> new EntitySpec(kind.path, kind.fields.stream().map(Field::spec).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DestinationOverview> destinations() {
        return repository.destinations(STALE_AFTER_DAYS);
    }

    @Transactional(readOnly = true)
    public Workspace workspace(String slug) {
        RecordView destination = repository.findDestination(slug)
                .orElseThrow(() -> AdminException.notFound("There's no destination called " + slug + "."));
        return new Workspace(destination,
                repository.findOwnedBy(Editable.PLACE, destination.id()),
                repository.findOwnedBy(Editable.HOTEL, destination.id()),
                repository.findLegsAtArrivalHubOf(destination.id()));
    }

    @Transactional(readOnly = true)
    public List<StaleRecord> stale() {
        return repository.stale(STALE_AFTER_DAYS);
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> history(@Nullable Editable kind, @Nullable Long id) {
        return repository.history(kind, id);
    }

    /** New records start as drafts, so nothing reaches visitors until a publisher publishes it. */
    @Transactional
    public RecordView create(AdminUser user, Editable kind, CreateRequest request) {
        if (kind == Editable.DESTINATION) {
            throw AdminException.invalid("Every district already has a destination. Edit it instead.");
        }
        Long destinationId = owner(kind, request.destinationSlug());
        Map<Field, @Nullable Object> values = convert(kind, request.values(), true);
        long id = write(() -> repository.insert(kind, values, destinationId));
        RecordView created = load(kind, id);
        repository.audit(user.email(), kind, id, "CREATE", null, created);
        return created;
    }

    @Transactional
    public RecordView save(AdminUser user, Editable kind, long id, SaveRequest request) {
        RecordView before = load(kind, id);
        if (user.role() != Role.PUBLISHER && VISIBLE.contains(before.status())) {
            throw AdminException.forbidden("Only a publisher can change what visitors see.");
        }
        Map<Field, @Nullable Object> values = convert(kind, request.values(), false);
        if (values.isEmpty()) {
            return before;
        }
        requireCurrent(write(() -> repository.update(kind, id, request.revision(), values)));
        return recordChange(user, kind, "UPDATE", before);
    }

    @Transactional
    public RecordView changeStatus(AdminUser user, Editable kind, long id, StatusRequest request) {
        if (user.role() != Role.PUBLISHER) {
            throw AdminException.forbidden("Only a publisher can publish, unpublish or archive.");
        }
        RecordView before = load(kind, id);
        String status = request.status().name();
        if (status.equals(before.status())) {
            return before;
        }
        requireCurrent(write(() -> repository.updateStatus(kind, id, request.revision(), status)));
        String action = switch (request.status()) {
            case PUBLISHED -> "PUBLISH";
            case DRAFT -> "UNPUBLISH";
            case ARCHIVED -> "ARCHIVE";
        };
        return recordChange(user, kind, action, before);
    }

    private RecordView recordChange(AdminUser user, Editable kind, String action, RecordView before) {
        RecordView after = load(kind, before.id());
        repository.audit(user.email(), kind, before.id(), action, before, after);
        if (VISIBLE.contains(before.status()) || VISIBLE.contains(after.status())) {
            repository.raiseCatalogVersion();
            events.publishEvent(new CatalogChanged());
        }
        return after;
    }

    /** Checks every field sent, so the admin sees all problems at once. */
    private Map<Field, @Nullable Object> convert(Editable kind, Map<String, @Nullable Object> raw, boolean creating) {
        Map<Field, @Nullable Object> values = new LinkedHashMap<>();
        Map<String, String> problems = new HashMap<>();
        raw.forEach((name, value) -> kind.field(name).ifPresentOrElse(field -> {
            if (field.createOnly() && !creating) {
                problems.put(name, "This can't change after the record is added.");
                return;
            }
            try {
                Object converted = field.convert(value);
                if (field.type() == FieldType.HUB && converted instanceof String slug && !repository.hubExists(slug)) {
                    problems.put(name, "Choose a place from the list.");
                    return;
                }
                values.put(field, converted);
            } catch (AdminException invalid) {
                problems.putAll(invalid.fields());
            }
        }, () -> problems.put(name, "This field doesn't exist.")));
        if (creating) {
            kind.fields.stream()
                    .filter(field -> field.required() && !raw.containsKey(field.name()))
                    .forEach(field -> problems.put(field.name(), "This can't be empty."));
        }
        if (!problems.isEmpty()) {
            throw AdminException.invalidFields(problems);
        }
        return values;
    }

    private @Nullable Long owner(Editable kind, @Nullable String slug) {
        if (!kind.belongsToDestination()) {
            return null;
        }
        if (slug == null) {
            throw AdminException.invalid("Choose the destination this belongs to.");
        }
        return repository.destinationId(slug)
                .orElseThrow(() -> AdminException.notFound("There's no destination called " + slug + "."));
    }

    private RecordView load(Editable kind, long id) {
        return repository.find(kind, id).orElseThrow(() -> AdminException.notFound("That record doesn't exist."));
    }

    private static void requireCurrent(boolean saved) {
        if (!saved) {
            throw AdminException.outOfDate();
        }
    }

    private static <T> T write(Supplier<T> change) {
        try {
            return change.get();
        } catch (DataIntegrityViolationException violation) {
            throw DataRules.explain(violation);
        }
    }

    /** Sent after a commit that changed what visitors see. */
    record CatalogChanged() {
    }
}
