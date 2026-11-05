package civsim.web.controller;

import civsim.dto.OrganizationComparisonDto;
import civsim.dto.PersonComparisonDto;
import civsim.model.Organization;
import civsim.model.Person;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Side-by-side comparison endpoints for characters and organizations.
 */
@RestController
@RequestMapping("/api/compare")
public class ComparisonController {

    private final SimulationRunner runner;

    public ComparisonController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/people")
    public ResponseEntity<PersonComparisonDto> comparePeople(
            @RequestParam(name = "id1") int id1,
            @RequestParam(name = "id2") int id2) {
        try {
            PersonComparisonDto comparison = runner.submitQuery(engine -> {
                Person p1 = engine.getPerson(id1);
                Person p2 = engine.getPerson(id2);
                if (p1 == null || p2 == null) return null;
                return SnapshotBuilder.buildPersonComparison(p1, p2, engine);
            }).get(3, TimeUnit.SECONDS);

            if (comparison == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/organizations")
    public ResponseEntity<OrganizationComparisonDto> compareOrganizations(
            @RequestParam(name = "id1") String id1,
            @RequestParam(name = "id2") String id2) {
        try {
            OrganizationComparisonDto comparison = runner.submitQuery(engine -> {
                Organization o1 = engine.getOrganizationSystem().getOrganization(id1);
                Organization o2 = engine.getOrganizationSystem().getOrganization(id2);
                if (o1 == null || o2 == null) return null;
                return SnapshotBuilder.buildOrganizationComparison(o1, o2, engine);
            }).get(3, TimeUnit.SECONDS);

            if (comparison == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

