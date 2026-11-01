package com.smarttourplanner.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;

/** A real PostgreSQL server for tests. It runs from a Maven dependency, so tests don't need Docker. */
public final class TestDatabase {

    private static final EmbeddedPostgres SERVER = start();

    private TestDatabase() {
    }

    /** Creates an empty database, so test classes can't affect each other. */
    public static DataSource createEmpty() {
        String name = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = SERVER.getPostgresDatabase().getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create test database", e);
        }
        return SERVER.getDatabase("postgres", name);
    }

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
    }
}
