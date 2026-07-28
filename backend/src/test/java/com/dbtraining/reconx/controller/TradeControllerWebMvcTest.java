package com.dbtraining.reconx.controller;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
class TradeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /*
     * @WebMvcTest loads the controller layer only.
     * The real service, mapper and database are deliberately not loaded.
     */
    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    /*
     * This prevents the MVC slice from trying to construct the real JWT
     * provider if Tickets 72–74 have already been implemented.
     */
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private TradeRequest validRequest() {
        return new TradeRequest(
                "TRD-20260315-9999",
                1L,
                1L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 15)
        );
    }

    @Test
    @WithMockUser(
            username = "trader@db.com",
            roles = "TRADER"
    )
    void testCreateTrade_authenticated_returns201() throws Exception {
        /*
         * The service normally returns a saved JPA entity. Mocking it avoids
         * loading JPA or a database in this controller-only test.
         */
        Trade savedTrade = mock(Trade.class);
        when(savedTrade.getId()).thenReturn(42L);

        Instant now = Instant.now();

        TradeResponse response = new TradeResponse(
                42L,
                "TRD-20260315-9999",
                1L,
                "SAP.DE",
                1L,
                "Apex Brokers Inc",
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 15),
                "PENDING",
                now,
                now
        );

        when(tradeService.create(
                any(TradeRequest.class),
                anyString()
        )).thenReturn(savedTrade);

        when(tradeMapper.toResponse(savedTrade))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/trades")
                                .contextPath("/api")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                validRequest()
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                containsString("/api/v1/trades/42")
                        )
                )
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(
                        jsonPath("$.tradeRef")
                                .value("TRD-20260315-9999")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );
    }

    @Test
    void testCreateTrade_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void testCreateTrade_viewerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }
}