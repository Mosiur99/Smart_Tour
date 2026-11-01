package com.smarttourplanner.catalog;

import com.smarttourplanner.catalog.CatalogModels.DestinationDetail;
import com.smarttourplanner.catalog.CatalogModels.DestinationPage;
import com.smarttourplanner.catalog.CatalogModels.Hub;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";
    // Browsers recheck after a minute. The CDN keeps a longer copy, which publishing will purge.
    private static final CacheControl HUBS = CacheControl.maxAge(Duration.ofMinutes(5))
            .sMaxAge(Duration.ofHours(1)).cachePublic();
    private static final CacheControl DESTINATIONS = CacheControl.maxAge(Duration.ofMinutes(1))
            .sMaxAge(Duration.ofMinutes(10)).cachePublic();

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/hubs")
    public ResponseEntity<List<Hub>> hubs() {
        return ResponseEntity.ok().cacheControl(HUBS).body(catalog.hubs());
    }

    @GetMapping("/destinations")
    public ResponseEntity<DestinationPage> destinations(
            @RequestParam(required = false) @Size(max = 60) String q,
            @RequestParam(required = false) @Pattern(regexp = SLUG) String division,
            @RequestParam(required = false) @Pattern(regexp = SLUG) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok().cacheControl(DESTINATIONS).body(catalog.destinations(q, division, cursor, limit));
    }

    @GetMapping("/destinations/{slug}")
    public ResponseEntity<DestinationDetail> destination(@PathVariable @Pattern(regexp = SLUG) String slug) {
        DestinationDetail destination = catalog.destination(slug)
                .orElseThrow(() -> new CatalogNotFoundException("We couldn't find a destination called " + slug + "."));
        return ResponseEntity.ok().cacheControl(DESTINATIONS).body(destination);
    }
}
