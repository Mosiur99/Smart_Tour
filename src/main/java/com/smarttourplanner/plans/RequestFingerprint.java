package com.smarttourplanner.plans;

import com.smarttourplanner.planner.PlannerModels.Interest;
import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

/** Gives the same hash to requests that mean the same thing, so they can share one plan. */
final class RequestFingerprint {

    private RequestFingerprint() {
    }

    static String of(PlanRequest request) {
        String interests = request.interests().stream()
                .map(Interest::name)
                .sorted()
                .collect(Collectors.joining(","));
        String canonical = String.join("|",
                request.originHub(), request.destination(), String.valueOf(request.days()),
                String.valueOf(request.people()), request.budget().name(), String.valueOf(request.month()), interests);
        return sha256(canonical);
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
