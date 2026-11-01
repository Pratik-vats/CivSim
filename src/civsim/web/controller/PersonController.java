package civsim.web.controller;

import civsim.dto.PagedResponse;
import civsim.dto.PersonDetailDto;
import civsim.dto.PersonSummaryDto;
import civsim.model.Person;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Read-only person query and inspection endpoints.
 */
@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final SimulationRunner runner;

    public PersonController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PersonSummaryDto>> listPeople(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "settlementId", required = false) Integer settlementId,
            @RequestParam(name = "alive", required = false) Boolean alive,
            @RequestParam(name = "awakened", required = false) Boolean awakened,
            @RequestParam(name = "supernaturalClass", required = false) String supernaturalClass,
            @RequestParam(name = "tier", required = false) Integer tier) {

        try {
            List<PersonSummaryDto> results = runner.submitQuery(engine -> {
                List<PersonSummaryDto> list = new ArrayList<>();
                for (Person p : engine.getAllPeople()) {
                    if (alive != null && p.isAlive() != alive) continue;
                    if (awakened != null && p.isAwakened() != awakened) continue;
                    if (settlementId != null && (p.getVillage() == null || p.getVillage().getId() != settlementId)) continue;
                    if (supernaturalClass != null && (p.getAetherProfile() == null || !supernaturalClass.equalsIgnoreCase(p.getAetherProfile().getSupernaturalClass().name()))) continue;
                    if (tier != null && (p.getAetherProfile() == null || p.getAetherProfile().getPowerTier() != tier)) continue;

                    list.add(SnapshotBuilder.buildPersonSummary(p));
                }
                return list;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(results, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDetailDto> getPersonDetail(@PathVariable("id") int id) {
        try {
            PersonDetailDto detail = runner.submitQuery(engine -> {
                Person target = null;
                for (Person p : engine.getAllPeople()) {
                    if (p.getId() == id) {
                        target = p;
                        break;
                    }
                }
                if (target == null) return null;
                return SnapshotBuilder.buildPersonDetail(target, engine.getOrganizationSystem());
            }).get(3, TimeUnit.SECONDS);

            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

