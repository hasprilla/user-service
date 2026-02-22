package com.sonifoy.user.infrastructure.persistence;

import org.springframework.boot.CommandLineRunner;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class DataMigrationRunner implements CommandLineRunner {

    private final DatabaseClient databaseClient;

    public DataMigrationRunner(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public void run(String... args) {
        log.info("Starting synchronous database migrations...");

        String[] queries = {
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS roles TEXT[]",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(100)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(100)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS device_data TEXT",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_type VARCHAR(50)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR(255)",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS last_session_duration BIGINT",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        };

        Flux.fromArray(queries)
                .flatMap(query -> databaseClient.sql(query)
                        .fetch()
                        .rowsUpdated()
                        .doOnSuccess(rows -> log.info("Successfully executed: {} (Rows: {})", query, rows))
                        .doOnError(error -> log.error("Failed to execute: {}. Error: {}", query, error.getMessage()))
                        .onErrorResume(e -> Mono.empty()))
                .collectList()
                .then(
                        databaseClient
                                .sql("SELECT column_name FROM information_schema.columns WHERE table_name = 'users'")
                                .map((row, metadata) -> row.get("column_name", String.class))
                                .all()
                                .collectList()
                                .doOnSuccess(columns -> log.info("Actual columns in 'users' table: {}", columns)))
                .block(Duration.ofMinutes(1));

        log.info("Finished synchronous database migrations.");
    }
}
