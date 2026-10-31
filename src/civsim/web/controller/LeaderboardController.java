package civsim.web.controller;

import civsim.dto.LeaderboardDto;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Historical and current civilization leaderboards.
 */
@RestController
@RequestMapping("/api/leaderboards")
public class LeaderboardController {

    private final SimulationRunner runner;

    public LeaderboardController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<LeaderboardDto> getLeaderboard(@RequestParam(defaultValue = "POWER") String category) {
        try {
            LeaderboardDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildLeaderboard(engine, category)
            ).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

