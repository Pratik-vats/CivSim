package civsim.web.controller;

import civsim.dto.FeudDetailDto;
import civsim.dto.FeudSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.FeudCase;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Phase 3.4 — Multi-generational Blood Feud ledger, grievance records, and escalation tracker.
 */
@RestController
@RequestMapping("/api/feuds")
public class FeudController {

    private final SimulationRunner runner;

    public FeudController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<FeudSummaryDto>> listFeuds(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "escalationLevel", required = false) String escalationLevel,
            @RequestParam(name = "clanId", required = false) String clanId,
            @RequestParam(name = "familyId", required = false) Integer familyId) {

        try {
            return runner.submitQuery(engine -> {
                List<FeudSummaryDto> list = new ArrayList<>();
                if (engine.getBloodFeudSystem() != null) {
                    for (FeudCase f : engine.getBloodFeudSystem().getAllFeuds()) {
                        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                            if (f.getCurrentFeudStatus() == null || !f.getCurrentFeudStatus().name().equalsIgnoreCase(status)) continue;
                        }
                        if (escalationLevel != null && !escalationLevel.isBlank() && !escalationLevel.equalsIgnoreCase("ALL")) {
                            if (f.getCurrentEscalationLevel() == null || !f.getCurrentEscalationLevel().name().equalsIgnoreCase(escalationLevel)) continue;
                        }
                        if (clanId != null && !clanId.isBlank()) {
                            boolean match = (f.getInitiatingOrgId() != null && f.getInitiatingOrgId().equalsIgnoreCase(clanId))
                                    || (f.getOpposingOrgId() != null && f.getOpposingOrgId().equalsIgnoreCase(clanId));
                            if (!match) continue;
                        }
                        if (familyId != null) {
                            boolean match = (f.getInitiatingFamilyId() != null && f.getInitiatingFamilyId().equals(familyId))
                                    || (f.getOpposingFamilyId() != null && f.getOpposingFamilyId().equals(familyId));
                            if (!match) continue;
                        }
                        list.add(SnapshotBuilder.buildFeudSummary(f, engine.getBloodFeudSystem()));
                    }
                }
                return ResponseEntity.ok(PagedResponse.of(list, page, size));
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeudDetailDto> getFeudDetail(@PathVariable("id") String id) {
        try {
            return runner.submitQuery(engine -> {
                if (engine.getBloodFeudSystem() == null) {
                    return ResponseEntity.notFound().<FeudDetailDto>build();
                }
                FeudCase f = engine.getBloodFeudSystem().getFeud(id);
                if (f == null) {
                    return ResponseEntity.notFound().<FeudDetailDto>build();
                }
                FeudDetailDto dto = SnapshotBuilder.buildFeudDetail(f, engine.getBloodFeudSystem());
                return ResponseEntity.ok(dto);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

