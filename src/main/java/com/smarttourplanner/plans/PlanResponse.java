package com.smarttourplanner.plans;

import java.time.OffsetDateTime;
import tools.jackson.databind.JsonNode;

public record PlanResponse(String publicId, JsonNode request, JsonNode plan, OffsetDateTime createdAt) {
}
