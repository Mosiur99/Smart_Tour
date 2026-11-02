package com.smarttourplanner.admin;

import java.util.Map;
import java.util.Optional;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Explains which database rule a change broke. The rules live in the database, so they hold for
 * every way data is written, and this only puts them into words.
 */
final class DataRules {

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("destination_complete_when_published",
                    "Add the Bangla name, map position and arrival hub before publishing."),
            Map.entry("poi_checked_when_published", "Add the Bangla name and checked date before publishing."),
            Map.entry("poi_routable_when_published",
                    "Sights and heritage places need a map position and visit time before publishing."),
            Map.entry("hotel_complete_when_published",
                    "Add the tier, map position, prices, source and checked date before publishing."),
            Map.entry("leg_complete_when_published",
                    "Add the travel time and checked date, and a source for the fare, before publishing."),
            Map.entry("poi_fee_range", "The highest fee can't be lower than the lowest."),
            Map.entry("hotel_price_range", "Give both prices, with the highest not lower than the lowest."),
            Map.entry("leg_fare_range", "Give both fares, with the highest not lower than the lowest."),
            Map.entry("leg_connects_two_hubs", "A journey needs two different places."));

    private static final String DUPLICATE = "Another record here already has this name.";
    private static final String UNKNOWN = "This change breaks a data rule.";

    private DataRules() {
    }

    static AdminException explain(DataIntegrityViolationException violation) {
        Optional<ServerErrorMessage> error = serverError(violation);
        String constraint = error.map(ServerErrorMessage::getConstraint).orElse(null);
        if (constraint == null) {
            return AdminException.breaksRule(UNKNOWN);
        }
        if (constraint.endsWith("_key")) {
            return AdminException.breaksRule(DUPLICATE);
        }
        return AdminException.breaksRule(MESSAGES.getOrDefault(constraint, UNKNOWN));
    }

    private static Optional<ServerErrorMessage> serverError(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException postgres && postgres.getServerErrorMessage() != null) {
                return Optional.of(postgres.getServerErrorMessage());
            }
        }
        return Optional.empty();
    }
}
