package com.smarttourplanner.plans;

import com.smarttourplanner.catalog.VersionedCache;
import com.smarttourplanner.planner.PlanEngine;
import com.smarttourplanner.planner.PlannerModels.Interest;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import com.smarttourplanner.planner.PlannerModels.TripPlan;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

    private final PlanRepository plans;
    private final PlanningDataRepository planningData;
    private final VersionedCache cache;
    private final PlanEngine engine = new PlanEngine();

    PlanService(PlanRepository plans, PlanningDataRepository planningData, VersionedCache cache) {
        this.plans = plans;
        this.planningData = planningData;
        this.cache = cache;
    }

    /** Returns the existing plan for this request when there is one, and creates it otherwise. */
    public PlanResult create(PlanRequestBody body) {
        PlanRequest request = toRequest(body);
        String hash = RequestFingerprint.of(request);
        int version = plans.currentCatalogVersion();

        Optional<PlanResponse> existing = plans.findByRequest(hash, version);
        if (existing.isPresent()) {
            return new PlanResult(existing.get(), false);
        }

        TripPlan plan = buildPlan(request);
        plans.insertIfAbsent(PublicIds.next(), hash, version, request, plan);
        return new PlanResult(plans.findByRequest(hash, version).orElseThrow(), true);
    }

    public Optional<PlanResponse> find(String publicId) {
        return plans.findByPublicId(publicId);
    }

    private TripPlan buildPlan(PlanRequest request) {
        if (!planningData.hubExists(request.originHub())) {
            throw new PlanNotFoundException("We don't know a starting point called " + request.originHub() + ".");
        }
        return engine.plan(request, destination(request.destination()), legs());
    }

    private TripDestination destination(String slug) {
        TripDestination destination = cache.get(
                "planning:destination:" + slug, () -> planningData.findPublishedDestination(slug).orElse(null));
        if (destination == null) {
            throw new PlanNotFoundException("Plans aren't available for " + slug + " yet.");
        }
        return destination;
    }

    private List<Leg> legs() {
        return cache.get("planning:legs", planningData::findPublishedLegs);
    }

    private static PlanRequest toRequest(PlanRequestBody body) {
        Set<Interest> interests = body.interests() != null ? body.interests() : Set.of();
        return new PlanRequest(
                body.originHub(), body.destination(), body.days(), body.people(), body.budget(), body.month(), interests);
    }

    public record PlanResult(PlanResponse plan, boolean created) {
    }
}
