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
    public ResponseEntity<LeaderboardDto> getLeaderboard(
            @RequestParam(name = "category", defaultValue = "POWER") String category,
            @RequestParam(name = "year", required = false) Integer year) {
        try {
            LeaderboardDto dto = runner.submitQuery(engine ->
                    SnapshotBuilder.buildLeaderboard(engine, category, year)
            ).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<LeaderboardDto> getLeaderboard(String category) {
        return getLeaderboard(category, null);
    }
}
