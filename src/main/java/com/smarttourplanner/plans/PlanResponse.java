package com.smarttourplanner.plans;

import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import com.smarttourplanner.planner.PlannerModels.TripPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import tools.jackson.databind.JsonNode;

/** The request and plan are stored as JSON, so the contract names the records they were written from. */
public record PlanResponse(
        String publicId,
        @Schema(implementation = PlanRequest.class) JsonNode request,
        @Schema(implementation = TripPlan.class) JsonNode plan,
        OffsetDateTime createdAt) {
}
