package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
/**
 * TICKET-ADV068 — POST /api/v1/recon/run — returns 202 + jobId
 * TICKET-ADV069 — GET  /api/v1/recon/jobs/{jobId}/results
 * TICKET-ADV070 — PUT  /api/v1/recon/results/{id}/resolve
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private final ReconBreakRepository breaks;

    public ReconController(ReconBreakRepository breaks) { this.breaks = breaks; }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job (async)")
    public ResponseEntity<Map<String, String>> runRecon(@Valid @RequestBody ReconRunRequest req) {
        // TODO(TICKET-ADV068): generate a jobId, write a row to recon_jobs, and
        //   return 202 Accepted with {"jobId": ..., "status": "QUEUED"}. A
        //   worker (Day 6 / Kafka consumer) picks the job up asynchronously.
        throw new UnsupportedOperationException("TICKET-ADV068");
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    public ResponseEntity<Page<ReconBreak>> results(
            @PathVariable String jobId,
            @PageableDefault(size = 50) Pageable pageable
    ) {

        Page<ReconBreak> result =
                breaks.findAll(pageable);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    public ResponseEntity<ReconBreak> resolve(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {

        ReconBreak rb = breaks.findById(id)
                .orElseThrow(
                        () -> new TradeNotFoundException(
                                "Break not found: " + id
                        )
                );


        String note = body.get("note");


        rb.resolve(note);


        ReconBreak saved = breaks.save(rb);


        return ResponseEntity.ok(saved);
    }
}
