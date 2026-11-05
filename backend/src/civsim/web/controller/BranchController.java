package civsim.web.controller;

import civsim.dto.BranchComparisonDto;
import civsim.dto.BranchSummaryDto;
import civsim.dto.CreateBranchRequest;
import civsim.persistence.WorldPersistenceService;
import civsim.runner.SimulationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Alternate timelines and world branching endpoints.
 * Allows forking an independent timeline from a validated historical checkpoint without mutating the parent world.
 */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final SimulationRunner runner;
    private final WorldPersistenceService persistenceService;

    public BranchController(SimulationRunner runner, WorldPersistenceService persistenceService) {
        this.runner = runner;
        this.persistenceService = persistenceService;
    }

    @GetMapping
    public ResponseEntity<List<BranchSummaryDto>> listBranches() {
        return ResponseEntity.ok(persistenceService.listBranches());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBranch(@RequestBody CreateBranchRequest req) {
        if (req.sourceSaveId() == null || req.sourceSaveId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sourceSaveId is required to fork a branch"));
        }
        try {
            BranchSummaryDto branch = persistenceService.createBranch(
                    req.sourceSaveId(),
                    req.branchName(),
                    req.rngPolicy()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(branch);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Source checkpoint could not be read: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/compare")
    public ResponseEntity<BranchComparisonDto> compareBranches(
            @RequestParam("world1") String world1,
            @RequestParam("world2") String world2) {
        BranchComparisonDto cmp = persistenceService.compareBranches(world1, world2);
        return ResponseEntity.ok(cmp);
    }
}

