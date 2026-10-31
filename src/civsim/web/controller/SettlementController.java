package civsim.web.controller;

import civsim.dto.PagedResponse;
import civsim.dto.SettlementDetailDto;
import civsim.dto.SettlementMarkerDto;
import civsim.model.Village;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Read-only settlement inspection endpoints.
 */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SimulationRunner runner;

    public SettlementController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<SettlementMarkerDto>> listSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var snap = runner.getLatestSnapshot();
        if (snap == null) return ResponseEntity.ok(PagedResponse.of(List.of(), page, size));

        return ResponseEntity.ok(PagedResponse.of(snap.settlements(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementDetailDto> getSettlementDetail(@PathVariable int id) {
        try {
            SettlementDetailDto detail = runner.submitQuery(engine -> {
                Village target = null;
                for (Village v : engine.getVillages()) {
                    if (v.getId() == id) {
                        target = v;
                        break;
                    }
                }
                if (target == null) return null;
                return SnapshotBuilder.buildSettlementDetail(target, engine.getVillages());
            }).get(3, TimeUnit.SECONDS);

            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

