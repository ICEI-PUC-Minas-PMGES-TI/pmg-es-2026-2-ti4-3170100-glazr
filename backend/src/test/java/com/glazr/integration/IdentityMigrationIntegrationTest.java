package com.glazr.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class IdentityMigrationIntegrationTest {

    private static final String EXISTING_HASH = "{bcrypt-sha256}$2a$12$existing.hash.is.copied.without.reencoding000000000000000";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-alpine3.24")
            .withDatabaseName("glazr_migration_test")
            .withUsername("glazr_test")
            .withPassword("glazr_test_password");

    @Test
    void upgradesVersionThreeWithoutLosingExistingPasswordCredential() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        Flyway.configure()
                .dataSource(dataSource)
                .target(MigrationVersion.fromVersion("3"))
                .load()
                .migrate();

        var jdbc = new JdbcTemplate(dataSource);
        UUID userId = UUID.fromString("252cd9f6-55cd-4c5e-a816-a62376a21a3d");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T10:00:00Z");
        jdbc.update("""
                insert into users (
                    id, email, display_name, password_hash, active,
                    failed_login_attempts, created_at, updated_at, version
                ) values (?, ?, ?, ?, true, 0, ?, ?, 0)
                """, userId, "existing@example.invalid", "Funcionário Existente",
                EXISTING_HASH, createdAt, createdAt);

        Flyway.configure().dataSource(dataSource).load().migrate();

        Integer credentialTableCount = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = 'user_password_credentials'
                """, Integer.class);
        assertThat(credentialTableCount).isEqualTo(1);

        assertThat(jdbc.queryForObject("""
                select password_hash from user_password_credentials where user_id = ?
                """, String.class, userId)).isEqualTo(EXISTING_HASH);
        assertThat(jdbc.queryForObject("""
                select account_type from users where id = ?
                """, String.class, userId)).isEqualTo("EMPLOYEE");

        Integer legacyColumnCount = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = 'users' and column_name = 'password_hash'
                """, Integer.class);
        assertThat(legacyColumnCount).isZero();
    }
}
