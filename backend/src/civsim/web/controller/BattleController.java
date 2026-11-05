package civsim.web.controller;

import civsim.dto.BattleDetailDto;
import civsim.dto.BattleParticipantDto;
import civsim.dto.BattleRoundRecordDto;
import civsim.dto.BattleRoundsDto;
import civsim.dto.BattleSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.BattleResult;
import civsim.model.Combatant;
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
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

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
    public ResponseEntity<BattleDetailDto> getBattleDetail(@PathVariable("id") String id) {
        BattleResult r = CombatEngine.getCachedBattle(id);
        if (r == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SnapshotBuilder.buildBattleDetail(r));
    }

    @GetMapping("/{id}/rounds")
    public ResponseEntity<BattleRoundsDto> getBattleRounds(@PathVariable("id") String id) {
        BattleResult r = CombatEngine.getCachedBattle(id);
        if (r == null) {
            return ResponseEntity.notFound().build();
        }
        List<BattleRoundRecordDto> rounds = CombatEngine.getCachedBattleRounds(id);
        List<Combatant> combatants = CombatEngine.getBattleParticipants(id);
        List<BattleParticipantDto> participants = new ArrayList<>();
        if (combatants != null) {
            for (Combatant c : combatants) {
                double initHp = c.getCombatState() != null ? c.getCombatState().getCurrentHealth() : 40.0;
                double maxHp = c.getCombatState() != null ? c.getCombatState().getMaxHealth() : 40.0;
                participants.add(new BattleParticipantDto(
                        c.getCombatantId(),
                        c.getName(),
                        c.getSideId(),
                        c.getEntityType() != null ? c.getEntityType().name() : "HUMAN",
                        c.getSupernaturalClass() != null ? c.getSupernaturalClass().name() : "NONE",
                        initHp,
                        maxHp,
                        c.getPowerTier(),
                        c.isAlive()
                ));
            }
        }
        if (rounds != null && !rounds.isEmpty()) {
            return ResponseEntity.ok(new BattleRoundsDto(id, true, null, rounds, participants));
        } else {
            return ResponseEntity.ok(new BattleRoundsDto(id, false, "Detailed replay unavailable for this historical battle", List.of(), participants));
        }
    }
}

