package civsim.web.controller;

import civsim.dto.WorldOverlaysDto;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Phase 3.6 — World overlays, heatmaps, territory boundaries, and spatial conflict endpoints.
 */
@RestController
@RequestMapping("/api")
public class OverlayController {

    private final SimulationRunner runner;

    public OverlayController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/world/overlays")
    public ResponseEntity<WorldOverlaysDto> getWorldOverlays() {
        try {
            return runner.submitQuery(engine -> {
                String worldId = runner.getWorldId();
                return ResponseEntity.ok(SnapshotBuilder.buildWorldOverlays(engine, worldId));
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/historical/overlays")
    public ResponseEntity<WorldOverlaysDto> getHistoricalOverlays(
            @RequestParam int year,
            @RequestParam(defaultValue = "0") int month) {
        try {
            return runner.submitQuery(engine -> {
                String worldId = runner.getWorldId();
                return ResponseEntity.ok(SnapshotBuilder.buildHistoricalOverlays(engine, year, month, worldId));
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
