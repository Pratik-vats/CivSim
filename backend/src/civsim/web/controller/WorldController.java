package civsim.web.controller;

import civsim.dto.*;
import civsim.persistence.WorldPersistenceService;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only live world data endpoints.
 */
@RestController
@RequestMapping("/api/world")
public class WorldController {

    private final SimulationRunner runner;
    @org.springframework.beans.factory.annotation.Autowired
    public WorldController(SimulationRunner runner) {
        this.runner = runner;
    }

    /** Retained for source compatibility with the in-repository integration harness. */
    public WorldController(SimulationRunner runner, WorldPersistenceService ignoredPersistenceService) {
        this(runner);
    }

    @GetMapping({"/snapshot", "/tick"})
    public ResponseEntity<WorldTickSnapshot> getSnapshot() {
        WorldTickSnapshot snap = runner.getLatestSnapshot();
        return (snap != null) ? ResponseEntity.ok(snap) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/summary")
    public ResponseEntity<WorldSummaryDto> getSummary() {
        WorldSummaryDto sum = runner.getLatestSummarySnapshot();
        return (sum != null) ? ResponseEntity.ok(sum) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/map")
    public ResponseEntity<WorldMapSnapshot> getMap() {
        WorldMapSnapshot map = runner.getLatestMapSnapshot();
        return (map != null) ? ResponseEntity.ok(map) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/search")
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<SearchResultDto>> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @RequestParam(name = "limit", required = false, defaultValue = "15") int limit) {
        try {
            List<SearchResultDto> results = (List<SearchResultDto>) runner.submitQuery(engine ->
                    SnapshotBuilder.searchEntities(engine, q, limit)
            ).get(2, java.util.concurrent.TimeUnit.SECONDS);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

}

