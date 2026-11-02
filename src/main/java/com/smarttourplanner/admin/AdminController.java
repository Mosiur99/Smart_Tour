package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminModels.AdminUser;
import com.smarttourplanner.admin.AdminModels.AuditEntry;
import com.smarttourplanner.admin.AdminModels.CreateRequest;
import com.smarttourplanner.admin.AdminModels.DestinationOverview;
import com.smarttourplanner.admin.AdminModels.EntitySpec;
import com.smarttourplanner.admin.AdminModels.RecordView;
import com.smarttourplanner.admin.AdminModels.SaveRequest;
import com.smarttourplanner.admin.AdminModels.StaleRecord;
import com.smarttourplanner.admin.AdminModels.StatusRequest;
import com.smarttourplanner.admin.AdminModels.Workspace;
import jakarta.validation.Valid;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The admin panel's API. {@link AdminGate} lets in only signed-in admins. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final String KIND = "{kind:destinations|places|hotels|legs}";
    private static final String SLUG = "{slug:[a-z0-9]+(?:-[a-z0-9]+)*}";

    private final AdminService admin;

    AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/me")
    public AdminUser me(@RequestAttribute(AdminGate.USER_ATTRIBUTE) AdminUser user) {
        return user;
    }

    /** The fields of each kind of record, so the panel can build its forms. */
    @GetMapping("/schema")
    public List<EntitySpec> schema() {
        return admin.schema();
    }

    @GetMapping("/destinations")
    public List<DestinationOverview> destinations() {
        return admin.destinations();
    }

    @GetMapping("/destinations/" + SLUG)
    public Workspace workspace(@PathVariable String slug) {
        return admin.workspace(slug);
    }

    @GetMapping("/stale")
    public List<StaleRecord> stale() {
        return admin.stale();
    }

    @GetMapping("/history")
    public List<AuditEntry> history(
            @RequestParam(required = false) @Nullable String kind, @RequestParam(required = false) @Nullable Long id) {
        return admin.history(kind == null ? null : editable(kind), id);
    }

    @PostMapping("/" + KIND)
    @ResponseStatus(HttpStatus.CREATED)
    public RecordView create(
            @RequestAttribute(AdminGate.USER_ATTRIBUTE) AdminUser user,
            @PathVariable String kind, @Valid @RequestBody CreateRequest request) {
        return admin.create(user, editable(kind), request);
    }

    @PutMapping("/" + KIND + "/{id:\\d{1,18}}")
    public RecordView save(
            @RequestAttribute(AdminGate.USER_ATTRIBUTE) AdminUser user,
            @PathVariable String kind, @PathVariable long id, @Valid @RequestBody SaveRequest request) {
        return admin.save(user, editable(kind), id, request);
    }

    @PostMapping("/" + KIND + "/{id:\\d{1,18}}/status")
    public RecordView changeStatus(
            @RequestAttribute(AdminGate.USER_ATTRIBUTE) AdminUser user,
            @PathVariable String kind, @PathVariable long id, @Valid @RequestBody StatusRequest request) {
        return admin.changeStatus(user, editable(kind), id, request);
    }

    private static Editable editable(String kind) {
        return Editable.fromPath(kind).orElseThrow(() -> AdminException.notFound("There's no kind called " + kind + "."));
    }
}
