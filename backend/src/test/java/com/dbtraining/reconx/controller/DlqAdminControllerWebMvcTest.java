package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.model.DlqMessage;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqAdminController.class)
class DlqAdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DlqMessageRepository dlqMessageRepository;

    @MockBean
    private TradeEventProducer tradeEventProducer;

    @Test
    @WithMockUser(roles = "ADMIN")
    void replayDryRun_asAdmin_returnsWouldReplayPayload() throws Exception {
        UUID eventId = UUID.randomUUID();
        TradeEvent tradeEvent = TradeEvent.created("TRD-100", objectMapper.createObjectNode().put("status", "replay-test"));
        DlqMessage message = new DlqMessage(eventId, "TRD-100", "trade-events", 0, 42L, tradeEvent, "bad payload", Instant.now());

        when(dlqMessageRepository.findByEventId(eventId)).thenReturn(Optional.of(message));

        mockMvc.perform(post("/api/v1/admin/dlq/replay")
                        .param("eventId", eventId.toString())
                        .param("dryRun", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.wouldReplayTo").value("trade-events"));
    }

    @Test
    void replayDryRun_unauthenticated_returns401() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/dlq/replay")
                        .param("eventId", eventId.toString())
                        .param("dryRun", "true")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void replayDryRun_nonAdmin_returns403() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/dlq/replay")
                        .param("eventId", eventId.toString())
                        .param("dryRun", "true")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
