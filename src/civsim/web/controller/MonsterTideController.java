package civsim.web.controller;

import civsim.dto.MonsterTideDetailDto;
import civsim.dto.MonsterTideSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.MonsterTide;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Phase 3.4 — MonsterTide invasion tracker and tactical breach analyzer.
 */
@RestController
@RequestMapping("/api/tides")
public class MonsterTideController {

    private final SimulationRunner runner;

    public MonsterTideController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<MonsterTideSummaryDto>> listTides(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "year", required = false) Integer year) {

        try {
            return runner.submitQuery(engine -> {
                List<MonsterTideSummaryDto> list = new ArrayList<>();
                long currentTotalMonths = engine.getTimeline().getTotalElapsedMonths();

                if (engine.getMonsterEcologySystem() != null) {
                    for (MonsterTide t : engine.getMonsterEcologySystem().getTides().values()) {
                        if (state != null && !state.isBlank() && !state.equalsIgnoreCase("ALL")) {
                            if (!t.getState().name().equalsIgnoreCase(state)) continue;
                        }
                        if (year != null) {
                            if (t.getMobilizedYear() != year && t.getArrivalYear() != year) continue;
                        }
                        list.add(SnapshotBuilder.buildMonsterTideSummary(t, engine.getMonsterEcologySystem(), currentTotalMonths));
                    }
                }
                return ResponseEntity.ok(PagedResponse.of(list, page, size));
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonsterTideDetailDto> getTideDetail(@PathVariable("id") String id) {
        try {
            return runner.submitQuery(engine -> {
                if (engine.getMonsterEcologySystem() == null) {
                    return ResponseEntity.notFound().<MonsterTideDetailDto>build();
                }
                MonsterTide t = engine.getMonsterEcologySystem().getTides().get(id);
                if (t == null) {
                    return ResponseEntity.notFound().<MonsterTideDetailDto>build();
                }
                long currentTotalMonths = engine.getTimeline().getTotalElapsedMonths();
                MonsterTideDetailDto dto = SnapshotBuilder.buildMonsterTideDetail(t, engine.getMonsterEcologySystem(), currentTotalMonths);
                return ResponseEntity.ok(dto);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

