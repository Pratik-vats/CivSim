package civsim.web.controller;

import civsim.SimulationConfig;
import civsim.dto.NewWorldRequest;
import civsim.model.ExtinctionPolicy;
import civsim.runner.SimulationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * World generation, seed control, and configuration options endpoints.
 */
@RestController
@RequestMapping("/api/worlds")
public class WorldCreationController {

    private final SimulationRunner runner;

    public WorldCreationController(SimulationRunner runner) {
        this.runner = runner;
    }

    @PostMapping("/new")
    public ResponseEntity<?> createNewWorld(@RequestBody(required = false) NewWorldRequest req) {
        long seed = (req != null && req.seed() != null) ? req.seed() : System.currentTimeMillis();
        String name = (req != null && req.worldName() != null && !req.worldName().isBlank())
                ? req.worldName().trim() : "World-" + seed;

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(seed);
        if (req != null && req.awakeningThreshold() != null) {
            int awakeningYear = Math.max(1, Math.min(200, req.awakeningThreshold()));
            config = config.withAwakeningStartYear(awakeningYear);
        }
        if (req != null && req.extinctionPolicy() != null) {
            try {
                config = config.withExtinctionPolicy(ExtinctionPolicy.valueOf(req.extinctionPolicy()));
            } catch (Exception ignored) {}
        }

        runner.createNewWorld(name, seed, config);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "worldId", runner.getWorldId(),
                "worldName", name,
                "seed", seed,
                "awakeningStartYear", config.getAwakeningStartYear(),
                "status", "INITIALIZED"
        ));
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        try {
            Map<String, Object> map = runner.submitQuery(engine -> Map.<String, Object>of(
                    "activeWorldId", runner.getWorldId(),
                    "seed", engine.getConfig().getRandomSeed(),
                    "worldWidth", civsim.spatial.WorldGeography.WIDTH,
                    "worldHeight", civsim.spatial.WorldGeography.HEIGHT,
                    "awakeningStartYear", engine.getConfig().getAwakeningStartYear(),
                    "extinctionPolicy", engine.getConfig().getExtinctionPolicy().name(),
                    "supportedExtinctionPolicies", List.of("STOP_ON_EXTINCTION", "CONTINUE_NONHUMAN_WORLD")
            )).get(2, TimeUnit.SECONDS);
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "activeWorldId", runner.getWorldId(),
                    "seed", 0L
            ));
        }
    }
}
