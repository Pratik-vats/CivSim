package civsim.web.controller;

import civsim.dto.CommandStatusDto;
import civsim.dto.WorldSaveDetailDto;
import civsim.persistence.WorldPersistenceService;
import civsim.runner.CommandType;
import civsim.runner.SimulationCommand;
import civsim.runner.SimulationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * World saves lifecycle, checkpoint validation, and persistence management endpoints.
 */
@RestController
@RequestMapping("/api/saves")
public class SaveManagerController {

    private final SimulationRunner runner;
    private final WorldPersistenceService persistenceService;

    public SaveManagerController(SimulationRunner runner, WorldPersistenceService persistenceService) {
        this.runner = runner;
        this.persistenceService = persistenceService;
    }

    @GetMapping
    public ResponseEntity<List<WorldSaveDetailDto>> listSaves() {
        return ResponseEntity.ok(persistenceService.listSaveDetails());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorldSaveDetailDto> getSaveDetail(@PathVariable("id") String saveId) {
        try {
            WorldSaveDetailDto detail = persistenceService.getSaveDetail(saveId);
            return ResponseEntity.ok(detail);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CommandStatusDto> createManualSave(@RequestBody(required = false) Map<String, String> payload) {
        String customId = (payload != null) ? payload.get("saveId") : null;
        SimulationCommand cmd = new SimulationCommand(CommandType.SAVE_WORLD, customId);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @PostMapping("/{id}/load")
    public ResponseEntity<CommandStatusDto> loadSave(@PathVariable("id") String saveId) {
        SimulationCommand cmd = new SimulationCommand(CommandType.LOAD_WORLD, saveId);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateSave(@PathVariable("id") String saveId) {
        boolean valid = persistenceService.validateSaveIntegrity(saveId);
        return ResponseEntity.ok(Map.of(
                "saveId", saveId,
                "isValid", valid,
                "isRestorable", valid,
                "status", valid ? "INTEGRITY_VERIFIED" : "CORRUPT_OR_UNSUPPORTED"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSave(@PathVariable("id") String saveId) {
        try {
            boolean deleted = persistenceService.deleteSave(saveId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("saveId", saveId, "deleted", true, "message", "Checkpoint removed."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("saveId", saveId, "error", "Save file not found."));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("saveId", saveId, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("saveId", saveId, "error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<Map<String, Object>> renameSave(
            @PathVariable("id") String saveId,
            @RequestBody Map<String, String> payload) {
        String newName = payload.get("name");
        try {
            boolean renamed = persistenceService.renameSave(saveId, newName);
            return (renamed)
                    ? ResponseEntity.ok(Map.of("saveId", saveId, "renamed", true, "name", newName))
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}

