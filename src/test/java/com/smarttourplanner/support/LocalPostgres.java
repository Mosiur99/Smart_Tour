package com.smarttourplanner.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;

/** Runs a throwaway PostgreSQL for local development, with no Docker or installation needed. */
public final class LocalPostgres {

    private static final int PORT = 54329;

    private LocalPostgres() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        try (EmbeddedPostgres ignored = EmbeddedPostgres.builder().setPort(PORT).start()) {
            System.out.println("PostgreSQL is ready: jdbc:postgresql://localhost:" + PORT + "/postgres");
            Thread.currentThread().join();
        }
    }
}
