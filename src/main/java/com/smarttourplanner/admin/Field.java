package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminModels.FieldSpec;
import com.smarttourplanner.admin.AdminModels.FieldType;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One editable column. It turns a value sent by the admin panel into a value for the database, or
 * explains what's wrong with it. The database checks stay the final word on what may be published.
 */
record Field(
        String name, String column, FieldType type, List<String> options, @Nullable String sqlType,
        @Nullable BigDecimal min, @Nullable BigDecimal max, boolean required, boolean createOnly) {

    private static final int TEXT_LIMIT = 200;
    private static final int LONG_TEXT_LIMIT = 2_000;
    private static final int URL_LIMIT = 500;
    private static final int TAG_LIMIT = 40;
    private static final int TAGS_PER_RECORD = 20;
    private static final LocalDate EARLIEST_CHECK = LocalDate.of(2000, 1, 1);
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    static Field text(String name, String column) {
        return new Field(name, column, FieldType.TEXT, List.of(), null, null, null, false, false);
    }

    static Field longText(String name, String column) {
        return new Field(name, column, FieldType.LONG_TEXT, List.of(), null, null, null, false, false);
    }

    static Field url(String name, String column) {
        return new Field(name, column, FieldType.URL, List.of(), null, null, null, false, false);
    }

    static Field integer(String name, String column, long min, long max) {
        return new Field(name, column, FieldType.INTEGER, List.of(), null,
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), false, false);
    }

    static Field decimal(String name, String column, String min, String max) {
        return new Field(name, column, FieldType.DECIMAL, List.of(), null,
                new BigDecimal(min), new BigDecimal(max), false, false);
    }

    static Field date(String name, String column) {
        return new Field(name, column, FieldType.DATE, List.of(), null, null, null, false, false);
    }

    static Field choice(String name, String column, String sqlType, List<String> options) {
        return new Field(name, column, FieldType.CHOICE, List.copyOf(options), sqlType, null, null, false, false);
    }

    static Field hub(String name, String column) {
        return new Field(name, column, FieldType.HUB, List.of(), null, null, null, false, false);
    }

    static Field numbers(String name, String column, int min, int max) {
        return new Field(name, column, FieldType.NUMBERS, List.of(), "smallint",
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), false, false);
    }

    static Field tags(String name, String column) {
        return new Field(name, column, FieldType.TAGS, List.of(), "text", null, null, false, false);
    }

    Field notEmpty() {
        return new Field(name, column, type, options, sqlType, min, max, true, createOnly);
    }

    /** Set when the record is added. Changing it later would make it a different record. */
    Field fixedOnceCreated() {
        return new Field(name, column, type, options, sqlType, min, max, required, true);
    }

    FieldSpec spec() {
        return new FieldSpec(name, type, options, min, max, required, createOnly);
    }

    /** The SQL that reads this column from the row aliased {@code t}, named after the field. */
    String selectSql() {
        String value = switch (type) {
            case HUB -> "(SELECT slug FROM hub WHERE id = t.%s)".formatted(column);
            case CHOICE -> "t.%s::text".formatted(column);
            default -> "t." + column;
        };
        return "%s AS \"%s\"".formatted(value, name);
    }

    /** The SQL that turns the named parameter into this column's type. */
    String valueSql(String parameter) {
        return switch (type) {
            case HUB -> "(SELECT id FROM hub WHERE slug = :%s)".formatted(parameter);
            case CHOICE -> "CAST(:%s AS %s)".formatted(parameter, sqlType);
            // Lists arrive as a JSON array, which keeps commas and quotes inside tags safe.
            case NUMBERS, TAGS -> "ARRAY(SELECT e::%s FROM jsonb_array_elements_text(CAST(:%s AS jsonb))"
                    .formatted(sqlType, parameter) + " WITH ORDINALITY AS item(e, i) ORDER BY i)";
            default -> ":" + parameter;
        };
    }

    /** Returns the value to store, or throws with a message the admin can act on. */
    @Nullable Object convert(@Nullable Object raw) {
        Object value = raw instanceof String text && text.isBlank() ? null : raw;
        if (value == null) {
            if (required) {
                throw invalid("This can't be empty.");
            }
            // List columns hold an empty list rather than null.
            return type == FieldType.NUMBERS || type == FieldType.TAGS ? List.of() : null;
        }
        return switch (type) {
            case TEXT -> text(value, TEXT_LIMIT);
            case LONG_TEXT -> text(value, LONG_TEXT_LIMIT);
            case URL -> url(value);
            case INTEGER -> inRange(wholeNumber(value));
            case DECIMAL -> inRange(decimal(value));
            case DATE -> checkedDate(value);
            case CHOICE -> choice(value);
            case HUB -> hubSlug(value);
            case NUMBERS -> numbers(value);
            case TAGS -> tags(value);
        };
    }

    private String text(Object value, int limit) {
        String text = string(value).strip();
        if (text.length() > limit) {
            throw invalid("Keep this under %d characters.".formatted(limit));
        }
        return text;
    }

    private String url(Object value) {
        String text = text(value, URL_LIMIT);
        try {
            URI uri = new URI(text);
            if ("https".equals(uri.getScheme()) && uri.getHost() != null) {
                return text;
            }
        } catch (java.net.URISyntaxException ignored) {
            // Reported below.
        }
        throw invalid("Use a full https:// address.");
    }

    private BigDecimal wholeNumber(Object value) {
        BigDecimal number = decimal(value);
        if (number.stripTrailingZeros().scale() > 0) {
            throw invalid("Use a whole number.");
        }
        return number;
    }

    private BigDecimal decimal(Object value) {
        if (!(value instanceof Number number)) {
            throw invalid("Use a number.");
        }
        return new BigDecimal(number.toString());
    }

    private Object inRange(BigDecimal number) {
        if (min != null && number.compareTo(min) < 0 || max != null && number.compareTo(max) > 0) {
            throw invalid("Use a number from %s to %s.".formatted(min.toPlainString(), max.toPlainString()));
        }
        return type == FieldType.DECIMAL ? number : number.intValueExact();
    }

    private LocalDate checkedDate(Object value) {
        try {
            LocalDate date = LocalDate.parse(string(value));
            if (date.isBefore(EARLIEST_CHECK) || date.isAfter(LocalDate.now())) {
                throw invalid("A checked date can't be in the future.");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw invalid("Use a date like 2026-10-31.");
        }
    }

    private String choice(Object value) {
        String text = string(value);
        if (!options.contains(text)) {
            throw invalid("Choose one of: " + String.join(", ", options) + ".");
        }
        return text;
    }

    private String hubSlug(Object value) {
        String text = string(value);
        if (!SLUG.matcher(text).matches()) {
            throw invalid("Choose a place from the list.");
        }
        return text;
    }

    private List<Integer> numbers(Object value) {
        List<Integer> numbers = new ArrayList<>();
        for (Object item : list(value)) {
            int number = (Integer) inRange(wholeNumber(item));
            if (!numbers.contains(number)) {
                numbers.add(number);
            }
        }
        if (required && numbers.isEmpty()) {
            throw invalid("This can't be empty.");
        }
        numbers.sort(null);
        return numbers;
    }

    private List<String> tags(Object value) {
        List<?> items = list(value);
        if (items.size() > TAGS_PER_RECORD) {
            throw invalid("Use at most %d tags.".formatted(TAGS_PER_RECORD));
        }
        List<String> tags = new ArrayList<>();
        for (Object item : items) {
            String tag = text(item, TAG_LIMIT);
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private List<?> list(Object value) {
        if (!(value instanceof List<?> items)) {
            throw invalid("Use a list.");
        }
        return items;
    }

    private String string(Object value) {
        if (!(value instanceof String text)) {
            throw invalid("Use text.");
        }
        return text;
    }

    private AdminException invalid(String message) {
        return AdminException.invalidField(name, message);
    }
}
