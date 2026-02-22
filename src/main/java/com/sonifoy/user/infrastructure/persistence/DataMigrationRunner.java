package com.sonifoy.user.infrastructure.persistence;

import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataMigrationRunner {

    private final DatabaseClient databaseClient;

    public DataMigrationRunner(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @PostConstruct
    public void runMigrations() {
        log.info("Running manual database migrations...");

        String[] queries = {
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS roles TEXT[]",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(100)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(100)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS device_data TEXT",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_type VARCHAR(50)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        };

        for (String query : queries) {
            databaseClient.sql(query)
                    .fetch()
                    .rowsUpdated()
                    .subscribe(
                            rows -> log.info("Successfully executed: {} (Rows updated: {})", query, rows),
                            error -> log.error("Failing to execute migration query: {}. Error: {}", query,
                                    error.getMessage()));
        }
    }
}
