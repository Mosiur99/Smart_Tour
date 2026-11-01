package com.smarttourplanner.plans;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
class PlanRepository {

    private static final String SELECT_PLAN = "SELECT public_id, request, result, created_at FROM trip_plan ";

    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    PlanRepository(JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    int currentCatalogVersion() {
        return jdbc.sql("SELECT version FROM catalog_version").query(Integer.class).single();
    }

    Optional<PlanResponse> findByPublicId(String publicId) {
        return jdbc.sql(SELECT_PLAN + "WHERE public_id = :publicId")
                .param("publicId", publicId)
                .query((rs, row) -> toResponse(rs.getString("public_id"), rs.getString("request"),
                        rs.getString("result"), rs.getObject("created_at", OffsetDateTime.class)))
                .optional();
    }

    Optional<PlanResponse> findByRequest(String requestHash, int catalogVersion) {
        return jdbc.sql(SELECT_PLAN + "WHERE request_hash = :hash AND catalog_version = :version")
                .param("hash", requestHash)
                .param("version", catalogVersion)
                .query((rs, row) -> toResponse(rs.getString("public_id"), rs.getString("request"),
                        rs.getString("result"), rs.getObject("created_at", OffsetDateTime.class)))
                .optional();
    }

    /** Saves the plan unless another request saved the same one first. */
    void insertIfAbsent(String publicId, String requestHash, int catalogVersion, Object request, Object result) {
        jdbc.sql("""
                INSERT INTO trip_plan (public_id, request_hash, catalog_version, request, result)
                VALUES (:publicId, :hash, :version, :request::jsonb, :result::jsonb)
                ON CONFLICT (request_hash, catalog_version) DO NOTHING
                """)
                .param("publicId", publicId)
                .param("hash", requestHash)
                .param("version", catalogVersion)
                .param("request", mapper.writeValueAsString(request))
                .param("result", mapper.writeValueAsString(result))
                .update();
    }

    private PlanResponse toResponse(String publicId, String request, String result, OffsetDateTime createdAt) {
        return new PlanResponse(publicId, mapper.readTree(request), mapper.readTree(result), createdAt);
    }
}
