package com.smarttourplanner.admin;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** An admin request that can't be done, with a message the admin can act on. */
public class AdminException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> fields;

    private AdminException(HttpStatus status, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.fields = fields;
    }

    static AdminException notSignedIn() {
        return new AdminException(HttpStatus.UNAUTHORIZED, "Sign in through Cloudflare Access first.", Map.of());
    }

    static AdminException forbidden(String message) {
        return new AdminException(HttpStatus.FORBIDDEN, message, Map.of());
    }

    static AdminException notFound(String message) {
        return new AdminException(HttpStatus.NOT_FOUND, message, Map.of());
    }

    static AdminException invalidField(String field, String message) {
        return invalidFields(Map.of(field, message));
    }

    static AdminException invalidFields(Map<String, String> fields) {
        return new AdminException(HttpStatus.BAD_REQUEST, "Fix the marked fields.", Map.copyOf(fields));
    }

    static AdminException invalid(String message) {
        return new AdminException(HttpStatus.BAD_REQUEST, message, Map.of());
    }

    static AdminException outOfDate() {
        return new AdminException(HttpStatus.CONFLICT,
                "Someone saved this record after you opened it. Reload to see their change, then try again.",
                Map.of());
    }

    static AdminException breaksRule(String message) {
        return new AdminException(HttpStatus.UNPROCESSABLE_CONTENT, message, Map.of());
    }

    public HttpStatus status() {
        return status;
    }

    /** Field name to message, for errors that belong to one field. */
    public Map<String, String> fields() {
        return fields;
    }
}
