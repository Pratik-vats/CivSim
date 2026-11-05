package civsim.web.controller;

import civsim.dto.ExpeditionDetailDto;
import civsim.dto.ExpeditionSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.Expedition;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Phase 3.4 — Expedition logistics, exploratory party tracking, and wilderness journeys.
 */
@RestController
@RequestMapping("/api/expeditions")
public class ExpeditionController {

    private final SimulationRunner runner;

    public ExpeditionController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ExpeditionSummaryDto>> listExpeditions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sponsorId", required = false) String sponsorId) {

        try {
            return runner.submitQuery(engine -> {
                List<ExpeditionSummaryDto> list = new ArrayList<>();
                if (engine.getAnomalySystem() != null) {
                    for (Expedition e : engine.getAnomalySystem().getAllExpeditions()) {
                        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                            if (e.getStatus() == null || !e.getStatus().name().equalsIgnoreCase(status)) continue;
                        }
                        if (sponsorId != null && !sponsorId.isBlank()) {
                            if (!e.getSponsorId().equalsIgnoreCase(sponsorId)) continue;
                        }
                        list.add(SnapshotBuilder.buildExpeditionSummary(e, engine));
                    }
                }
                return ResponseEntity.ok(PagedResponse.of(list, page, size));
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpeditionDetailDto> getExpeditionDetail(@PathVariable("id") String id) {
        try {
            return runner.submitQuery(engine -> {
                if (engine.getAnomalySystem() == null) {
                    return ResponseEntity.notFound().<ExpeditionDetailDto>build();
                }
                Expedition exp = engine.getAnomalySystem().getExpedition(id);
                if (exp == null) {
                    return ResponseEntity.notFound().<ExpeditionDetailDto>build();
                }
                ExpeditionDetailDto dto = SnapshotBuilder.buildExpeditionDetail(exp, engine);
                return ResponseEntity.ok(dto);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

