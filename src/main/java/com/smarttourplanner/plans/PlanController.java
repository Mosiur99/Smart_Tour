package com.smarttourplanner.plans;

import com.smarttourplanner.plans.PlanService.PlanResult;
import com.smarttourplanner.security.ClientAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private static final String PUBLIC_ID = "^[2-9a-zA-Z]{12}$";
    // A saved plan never changes, so shared links can be cached for a long time.
    private static final CacheControl SAVED_PLAN = CacheControl.maxAge(Duration.ofHours(1))
            .sMaxAge(Duration.ofDays(1)).cachePublic();

    private final PlanService plans;
    private final ClientAddress clientAddress;

    public PlanController(PlanService plans, ClientAddress clientAddress) {
        this.plans = plans;
        this.clientAddress = clientAddress;
    }

    @PostMapping
    public ResponseEntity<PlanResponse> create(
            @Valid @RequestBody PlanRequestBody request, HttpServletRequest httpRequest) {
        PlanResult result = plans.create(request, clientAddress.of(httpRequest));
        URI location = URI.create("/api/v1/plans/" + result.plan().publicId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).location(location).body(result.plan());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<PlanResponse> read(@PathVariable @Pattern(regexp = PUBLIC_ID) String publicId) {
        PlanResponse plan = plans.find(publicId)
                .orElseThrow(() -> new PlanNotFoundException("We couldn't find that plan. Check the link and try again."));
        return ResponseEntity.ok().cacheControl(SAVED_PLAN).body(plan);
    }
}
