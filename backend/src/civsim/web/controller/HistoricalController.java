package civsim.web.controller;

import civsim.dto.SettlementHistoryDto;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/** Retained read-only settlement history endpoint for the live settlement inspector. */
@RestController
@RequestMapping("/api/historical/settlements")
public class HistoricalController {
    private final SimulationRunner runner;

    public HistoricalController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<SettlementHistoryDto> getSettlementHistory(@PathVariable("id") int settlementId) {
        try {
            SettlementHistoryDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildSettlementHistory(engine, settlementId)
            ).get(2, TimeUnit.SECONDS);
            return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
