package civsim.web.controller;

import civsim.dto.*;
import civsim.persistence.WorldPersistenceService;
import civsim.runner.CommandType;
import civsim.runner.SimulationCommand;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * World read model and persistence endpoints.
 */
@RestController
@RequestMapping("/api/world")
public class WorldController {

    private final SimulationRunner runner;
    private final WorldPersistenceService persistenceService;

    public WorldController(SimulationRunner runner, WorldPersistenceService persistenceService) {
        this.runner = runner;
        this.persistenceService = persistenceService;
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

    @PostMapping("/save")
    public ResponseEntity<CommandStatusDto> saveWorld(@RequestBody(required = false) Map<String, String> payload) {
        String customId = (payload != null) ? payload.get("saveId") : null;
        SimulationCommand cmd = new SimulationCommand(CommandType.SAVE_WORLD, customId);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @PostMapping("/load/{saveId}")
    public ResponseEntity<CommandStatusDto> loadWorld(@PathVariable("saveId") String saveId) {
        SimulationCommand cmd = new SimulationCommand(CommandType.LOAD_WORLD, saveId);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @GetMapping("/saves")
    public ResponseEntity<List<WorldSaveSummaryDto>> listSaves() {
        return ResponseEntity.ok(persistenceService.listSaves());
    }
}

