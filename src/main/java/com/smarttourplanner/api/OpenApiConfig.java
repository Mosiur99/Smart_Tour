package com.smarttourplanner.api;

import com.smarttourplanner.catalog.CatalogModels;
import com.smarttourplanner.planner.PlannerModels;
import com.smarttourplanner.plans.PlanResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes the published contract say exactly what the JSON contains, so the
 * frontend's generated types are exact. Jackson writes every field, nulls
 * included, so every response property is present. A property can be null
 * only when its record component is marked {@link Nullable}.
 */
@Configuration
class OpenApiConfig {

    /** Request bodies keep the required list that their validation rules produce. */
    private static final Set<String> REQUEST_BODIES = Set.of("PlanRequestBody");

    @Bean
    OpenApiCustomizer exactResponseShapes() {
        return OpenApiConfig::applyNullability;
    }

    @SuppressWarnings("rawtypes")
    static void applyNullability(OpenAPI api) {
        Map<String, Class<?>> records = responseRecords();
        api.getComponents().getSchemas().forEach((name, schema) -> {
            Class<?> record = records.get(name);
            Map<String, Schema> properties = schema.getProperties();
            if (REQUEST_BODIES.contains(name) || record == null || properties == null) {
                return;
            }
            schema.setRequired(new ArrayList<>(properties.keySet()));
            for (RecordComponent component : record.getRecordComponents()) {
                if (component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                    properties.computeIfPresent(component.getName(), (key, property) -> allowNull(property));
                }
            }
        });
    }

    /** Every record the API returns, found by walking from the response types, keyed by schema name. */
    static Map<String, Class<?>> responseRecords() {
        Map<String, Class<?>> found = new HashMap<>();
        Stream.of(CatalogModels.class.getDeclaredClasses(), PlannerModels.class.getDeclaredClasses())
                .flatMap(Stream::of)
                .forEach(type -> collect(type, found));
        collect(PlanResponse.class, found);
        return found;
    }

    private static void collect(Type type, Map<String, Class<?>> found) {
        if (type instanceof ParameterizedType generic) {
            Stream.of(generic.getActualTypeArguments()).forEach(argument -> collect(argument, found));
        } else if (type instanceof Class<?> plain && plain.isRecord() && !found.containsValue(plain)) {
            found.put(schemaName(plain), plain);
            Stream.of(plain.getRecordComponents()).forEach(component -> collect(component.getGenericType(), found));
        }
    }

    private static String schemaName(Class<?> record) {
        io.swagger.v3.oas.annotations.media.Schema named =
                record.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        return named != null && !named.name().isEmpty() ? named.name() : record.getSimpleName();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema allowNull(Schema property) {
        if (property.get$ref() != null) {
            return new JsonSchema().anyOf(List.of(new JsonSchema().$ref(property.get$ref()), nullSchema()));
        }
        property.addType("null");
        return property;
    }

    private static Schema<?> nullSchema() {
        JsonSchema schema = new JsonSchema();
        schema.addType("null");
        return schema;
    }
}
