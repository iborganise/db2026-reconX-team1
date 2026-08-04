package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

/**
 * ============================================================================
 * TICKET-ADV059 — DatabaseHealthIndicator (timed SELECT 1)
 *
 * WHAT: Custom actuator HealthIndicator that runs a fast `SELECT 1` with
 * a 2-second timeout and reports latencyMs as a detail.
 * HOW: Extends AbstractHealthIndicator; Spring picks it up by bean name
 * and exposes it under /actuator/health/database.
 * WHY: The default DataSource health indicator works, but a custom one
 * gives us a controllable timeout AND visible latency for SRE
 * dashboards.
 * OBSERVE: GET /api/actuator/health/database -> `{"status":"UP",
 * "details":{"latencyMs": <number>}}`.
 * ============================================================================
 *
 * TODO(TICKET-ADV059):
 * long start = System.nanoTime();
 * try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
 * s.setQueryTimeout(2);
 * s.execute("SELECT 1");
 * builder.up().withDetail("latencyMs", (System.nanoTime() - start) /
 * 1_000_000);
 * }
 *
 * HINT: Throw any exception out of this method — AbstractHealthIndicator
 * converts it to DOWN with the exception class as a detail.
 * ============================================================================
 */
@Component("reconxDatabase")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        super("ReconX database health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        long start = System.nanoTime();
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement()) {
            s.setQueryTimeout((int) TIMEOUT.getSeconds());
            try (ResultSet rs = s.executeQuery("SELECT 1")) {
                if (!rs.next()) {
                    throw new IllegalStateException("SELECT 1 returned no rows");
                }
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            builder.up()
                    .withDetail("query", "SELECT 1")
                    .withDetail("elapsedMs", elapsedMs);
        } catch (SQLException e) {
            builder.down(e).withDetail("query", "SELECT 1");
        }

    }
}
