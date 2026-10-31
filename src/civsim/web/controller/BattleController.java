package civsim.web.controller;

import civsim.dto.BattleDetailDto;
import civsim.dto.BattleSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.BattleResult;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import civsim.systems.CombatEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Historical battle query and tactical encounter inspection endpoints.
 */
@RestController
@RequestMapping("/api/battles")
public class BattleController {

    private final SimulationRunner runner;

    public BattleController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<BattleSummaryDto>> listBattles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<BattleResult> cached = CombatEngine.getAllCachedBattles();
        List<BattleSummaryDto> dtos = new ArrayList<>();
        for (BattleResult r : cached) {
            dtos.add(new BattleSummaryDto(
                    r.getBattleId(),
                    r.getSimulationYear(),
                    r.getSimulationMonth(),
                    r.getBattleType() != null ? r.getBattleType().name() : "SKIRMISH",
                    r.getOutcome() != null ? r.getOutcome().name() : "VICTORY",
                    r.getVictoriousSideId(),
                    r.getTotalRounds(),
                    r.getFatalities().size(),
                    r.getIncapacitated().size(),
                    r.getEscaped().size(),
                    r.getSummaryText()
            ));
        }

        return ResponseEntity.ok(PagedResponse.of(dtos, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BattleDetailDto> getBattleDetail(@PathVariable String id) {
        BattleResult r = CombatEngine.getCachedBattle(id);
        if (r == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SnapshotBuilder.buildBattleDetail(r));
    }
}

