package com.dbtraining.reconx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeLifecycleIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    private static String token;
    private static Long createdId;
    private static String reconJobId;
    private static Long breakId;

    /*
     * Using today's date ensures the trade falls inside one of the rolling
     * PostgreSQL partitions created by Liquibase.
     */
    private static final LocalDate TRADE_DATE = LocalDate.now();

    private static final String TRADE_REF =
            "INT-"
                    + TRADE_DATE.format(
                    DateTimeFormatter.BASIC_ISO_DATE
            )
                    + "-0001";

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    @Order(1)
    void loginAsAdmin() {
        Map<String, String> body = Map.of(
                "email", "admin@db.com",
                "password", "admin123"
        );

        ResponseEntity<JsonNode> response = http.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()),
                JsonNode.class
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        token = response.getBody()
                .path("token")
                .asText();

        assertFalse(token.isBlank());
    }

    @Test
    @Order(2)
    void createTrade() {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("tradeRef", TRADE_REF);
        body.put("instrumentId", 1L);
        body.put("counterpartyId", 1L);
        body.put("assetClass", "EQUITY");
        body.put("side", "BUY");
        body.put("quantity", 100.0);
        body.put("price", 245.50);
        body.put("tradeDate", TRADE_DATE.toString());

        ResponseEntity<JsonNode> response = http.exchange(
                "/v1/trades",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()),
                JsonNode.class
        );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        createdId = response.getBody()
                .path("id")
                .asLong();

        assertTrue(createdId > 0);

        assertEquals(
                "PENDING",
                response.getBody()
                        .path("status")
                        .asText()
        );
    }

    @Test
    @Order(3)
    void listReturnsCreatedTrade() {
        ResponseEntity<JsonNode> response = http.exchange(
                "/v1/trades?status=PENDING",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                JsonNode.class
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertTrue(
                response.getBody()
                        .path("totalElements")
                        .asLong() >= 1
        );
    }

    @Test
    @Order(4)
    void patchStatus() {
        Map<String, String> body =
                Map.of("status", "MATCHED");

        ResponseEntity<JsonNode> response = http.exchange(
                "/v1/trades/"
                        + createdId
                        + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders()),
                JsonNode.class
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "MATCHED",
                response.getBody()
                        .path("status")
                        .asText()
        );
    }

    @Test
    @Order(5)
    void triggerRecon() {
        LocalDate from =
                TRADE_DATE.withDayOfMonth(1);

        Map<String, Object> body = Map.of(
                "from", from.toString(),
                "to", TRADE_DATE.toString(),
                "counterpartyId", 1L
        );

        ResponseEntity<JsonNode> response = http.exchange(
                "/v1/recon/run",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()),
                JsonNode.class
        );

        assertEquals(
                HttpStatus.ACCEPTED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        reconJobId = response.getBody()
                .path("jobId")
                .asText();

        assertFalse(reconJobId.isBlank());
    }

    @Test
    @Order(6)
    void resolveBreak() {
        /*
         * The latest repository does not seed a recon break.
         * Create one directly as integration-test preparation.
         */
        breakId = jdbc.queryForObject(
                """
                INSERT INTO recon_breaks
                    (
                        trade_id,
                        discrepancy_type,
                        status,
                        detected_at
                    )
                VALUES
                    (
                        ?,
                        'PRICE_MISMATCH',
                        'OPEN',
                        CURRENT_TIMESTAMP
                    )
                RETURNING id
                """,
                Long.class,
                createdId
        );

        Map<String, String> body = Map.of(
                "note",
                "Confirmed during integration test."
        );

        ResponseEntity<JsonNode> response = http.exchange(
                "/v1/recon/results/"
                        + breakId
                        + "/resolve",
                HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders()),
                JsonNode.class
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "RESOLVED",
                response.getBody()
                        .path("status")
                        .asText()
        );
    }
}