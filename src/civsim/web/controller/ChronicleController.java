package civsim.web.controller;

import civsim.AnnualReport;
import civsim.dto.AnnualChronicleDto;
import civsim.dto.CivilizationMilestoneDto;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Phase 3.4 — Historical Chronicle, Annual Demographics, and Civilization Milestones.
 */
@RestController
@RequestMapping("/api/chronicle")
public class ChronicleController {

    private final SimulationRunner runner;

    public ChronicleController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/years")
    public ResponseEntity<Map<String, Object>> getRecordedYears() {
        try {
            return runner.submitQuery(engine -> {
                List<Integer> years = new ArrayList<>();
                for (AnnualReport r : engine.getAnnualReports()) {
                    years.add(r.getYear());
                }
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("years", years);
                resp.put("currentYear", engine.getTimeline().getYear());
                resp.put("currentMonth", engine.getTimeline().getMonth());
                resp.put("awakeningYear", engine.getConfig().getAwakeningStartYear());
                return ResponseEntity.ok(resp);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{year}")
    public ResponseEntity<AnnualChronicleDto> getAnnualChronicle(@PathVariable("year") int year) {
        try {
            return runner.submitQuery(engine -> {
                AnnualReport targetReport = null;
                for (AnnualReport r : engine.getAnnualReports()) {
                    if (r.getYear() == year) {
                        targetReport = r;
                        break;
                    }
                }
                if (targetReport == null) {
                    return ResponseEntity.notFound().<AnnualChronicleDto>build();
                }
                AnnualChronicleDto dto = SnapshotBuilder.buildAnnualChronicle(
                        targetReport,
                        engine.getAnnualReports(),
                        engine.getTimeline(),
                        engine.getConfig(),
                        engine
                );
                return ResponseEntity.ok(dto);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/milestones")
    public ResponseEntity<List<CivilizationMilestoneDto>> getMilestones() {
        try {
            return runner.submitQuery(engine -> {
                List<CivilizationMilestoneDto> milestones = SnapshotBuilder.buildCivilizationMilestones(engine);
                return ResponseEntity.ok(milestones);
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

