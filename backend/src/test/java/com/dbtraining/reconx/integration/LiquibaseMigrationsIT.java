package com.dbtraining.reconx.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class LiquibaseMigrationsIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void liquibaseCreatesExpectedSchemaAndSeedData() {
        Integer appliedChangesets = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM databasechangelog
                """,
                Integer.class
        );

        assertThat(appliedChangesets)
                .isNotNull()
                .isGreaterThanOrEqualTo(18);

        Integer counterparties = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM counterparties
                """,
                Integer.class
        );

        assertThat(counterparties)
                .isEqualTo(10);

        Integer instruments = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM instruments
                """,
                Integer.class
        );

        assertThat(instruments)
                .isEqualTo(15);

        Integer users = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM users
                """,
                Integer.class
        );

        assertThat(users)
                .isEqualTo(4);

        Integer requiredTables = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'counterparties',
                      'instruments',
                      'trades',
                      'settlements',
                      'audit_log',
                      'recon_breaks',
                      'recon_jobs',
                      'users'
                  )
                """,
                Integer.class
        );

        assertThat(requiredTables)
                .isEqualTo(8);

        Integer deletedAtColumns = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'trades'
                  AND column_name = 'deleted_at'
                """,
                Integer.class
        );

        assertThat(deletedAtColumns)
                .isEqualTo(1);

        Integer partitionedTradeTables = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_partitioned_table
                WHERE partrelid = 'public.trades'::regclass
                """,
                Integer.class
        );

        assertThat(partitionedTradeTables)
                .isEqualTo(1);

        Integer tradePartitions = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_inherits
                WHERE inhparent = 'public.trades'::regclass
                """,
                Integer.class
        );

        assertThat(tradePartitions)
                .isGreaterThan(0);
    }
}