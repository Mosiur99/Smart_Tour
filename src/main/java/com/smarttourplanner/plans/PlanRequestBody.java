package com.smarttourplanner.plans;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.Interest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** What a traveller sends to create a plan. There's no free text, so nothing reaches the AI step unchecked. */
public record PlanRequestBody(
        @NotNull @Pattern(regexp = SLUG) String originHub,
        @NotNull @Pattern(regexp = SLUG) String destination,
        @Min(1) @Max(14) int days,
        @Min(1) @Max(30) int people,
        @NotNull BudgetLevel budget,
        @Min(1) @Max(12) int month,
        @Size(max = 5) Set<Interest> interests) {

    private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";
}
