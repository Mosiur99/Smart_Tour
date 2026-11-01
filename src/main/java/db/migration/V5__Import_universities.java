package db.migration;

import java.io.IOException;
import java.io.InputStream;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** One-time import of university data, which the district and tour pages show. */
public class V5__Import_universities extends BaseJavaMigration {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void migrate(Context context) throws IOException {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        for (JsonNode summary : read("data/districts-index.json")) {
            String slug = summary.get("id").asString();
            JsonNode district = read("data/districts/" + slug + ".json");
            jdbc.update("UPDATE destination SET university_note = ? WHERE slug = ?",
                    text(district, "universityNote"), slug);
            for (JsonNode university : district.path("popularUniversities")) {
                jdbc.update("""
                        INSERT INTO university (destination_id, name_en, kind, location, note)
                        VALUES ((SELECT id FROM destination WHERE slug = ?), ?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                        """,
                        slug, university.get("name").asString(), text(university, "type"),
                        text(university, "location"), text(university, "note"));
            }
        }
    }

    private JsonNode read(String path) throws IOException {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return mapper.readTree(input);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
