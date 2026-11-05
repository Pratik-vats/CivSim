package civsim.web.controller;

import civsim.dto.AnomalyContestDto;
import civsim.dto.AnomalyDetailDto;
import civsim.dto.PagedResponse;
import civsim.model.WorldAnomaly;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Phase 3.4 — World Anomaly sites, territorial conflicts, and supernatural deposit recovery.
 */
@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final SimulationRunner runner;

    public AnomalyController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AnomalyDetailDto>> listAnomalies(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "grade", required = false) String grade) {

        try {
            return runner.submitQuery(engine -> {
                List<AnomalyDetailDto> list = new ArrayList<>();
                if (engine.getAnomalySystem() != null) {
                    for (WorldAnomaly a : engine.getAnomalySystem().getAllAnomalies()) {
                        // Strict visibility rule: never expose unrevealed forming anomalies
                        if (!a.isPubliclyObservable()) continue;

                        if (state != null && !state.isBlank() && !state.equalsIgnoreCase("ALL")) {
                            if (a.getState() == null || !a.getState().name().equalsIgnoreCase(state)) continue;
                        }
                        if (grade != null && !grade.isBlank() && !grade.equalsIgnoreCase("ALL")) {
                            if (a.getResourceGrade() == null || !a.getResourceGrade().name().equalsIgnoreCase(grade)) continue;
                        }
                        list.add(SnapshotBuilder.buildAnomalyDetail(a, engine.getAnomalySystem()));
                    }
                }
                return ResponseEntity.ok(PagedResponse.of(list, page, size));
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnomalyDetailDto> getAnomalyDetail(@PathVariable("id") String id) {
        try {
            return runner.submitQuery(engine -> {
                if (engine.getAnomalySystem() == null) {
                    return ResponseEntity.notFound().<AnomalyDetailDto>build();
                }
                WorldAnomaly a = engine.getAnomalySystem().getAnomaly(id);
                // Strict visibility rule: 404 if not found or if not observable
                if (a == null || !a.isPubliclyObservable()) {
                    return ResponseEntity.notFound().<AnomalyDetailDto>build();
                }
                AnomalyDetailDto dto = SnapshotBuilder.buildAnomalyDetail(a, engine.getAnomalySystem());
                return ResponseEntity.ok(dto);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/contests")
    public ResponseEntity<List<AnomalyContestDto>> getAnomalyContests(@PathVariable("id") String id) {
        try {
            return runner.submitQuery(engine -> {
                if (engine.getAnomalySystem() == null) {
                    return ResponseEntity.ok(List.<AnomalyContestDto>of());
                }
                WorldAnomaly a = engine.getAnomalySystem().getAnomaly(id);
                if (a == null || !a.isPubliclyObservable()) {
                    return ResponseEntity.notFound().<List<AnomalyContestDto>>build();
                }
                List<AnomalyContestDto> contests = SnapshotBuilder.buildAnomalyContests(id, engine.getAnomalySystem());
                return ResponseEntity.ok(contests);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

