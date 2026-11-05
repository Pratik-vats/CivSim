package civsim.web.controller;

import civsim.dto.GraphDataDto;
import civsim.dto.PagedResponse;
import civsim.dto.PersonDetailDto;
import civsim.dto.PersonSummaryDto;
import civsim.model.AetherProfile;
import civsim.model.Organization;
import civsim.model.Person;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Read-only person query, character inspection, and genealogy tree endpoints.
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
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "settlementId", required = false) Integer settlementId,
            @RequestParam(name = "familyId", required = false) Integer familyId,
            @RequestParam(name = "clanId", required = false) String clanId,
            @RequestParam(name = "alive", required = false) Boolean alive,
            @RequestParam(name = "awakened", required = false) Boolean awakened,
            @RequestParam(name = "supernaturalClass", required = false) String supernaturalClass,
            @RequestParam(name = "tier", required = false) Integer tier,
            @RequestParam(name = "sortBy", defaultValue = "tier") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        try {
            List<PersonSummaryDto> results = runner.submitQuery(engine -> {
                List<Person> candidates = (alive != null && alive)
                        ? engine.getAllPeople()
                        : engine.getAllPeopleIncludingDeceased();

                List<Person> filtered = new ArrayList<>();
                String query = (search != null) ? search.trim().toLowerCase() : null;

                for (Person p : candidates) {
                    if (alive != null && p.isAlive() != alive) continue;
                    if (awakened != null && p.isAwakened() != awakened) continue;
                    if (settlementId != null && (p.getVillage() == null || p.getVillage().getId() != settlementId)) continue;
                    if (familyId != null && (p.getFamily() == null || p.getFamily().getId() != familyId)) continue;
                    if (supernaturalClass != null && (p.getAetherProfile() == null || !supernaturalClass.equalsIgnoreCase(p.getAetherProfile().getSupernaturalClass().name()))) continue;
                    if (tier != null && (p.getAetherProfile() == null || p.getAetherProfile().getPowerTier() != tier)) continue;

                    if (clanId != null && !clanId.isBlank()) {
                        Organization org = engine.getOrganizationSystem().getOrganization(clanId);
                        if (org == null || org.getMembership(p.getId()) == null) continue;
                    }

                    if (query != null && !query.isEmpty()) {
                        boolean match = p.getName().toLowerCase().contains(query)
                                || String.valueOf(p.getId()).equals(query)
                                || (p.getFamily() != null && p.getFamily().getSurname().toLowerCase().contains(query))
                                || (p.getVillage() != null && p.getVillage().getName().toLowerCase().contains(query));
                        if (!match) continue;
                    }

                    filtered.add(p);
                }

                // Sorting
                Comparator<Person> comparator = switch (sortBy.toLowerCase()) {
                    case "name" -> Comparator.comparing(Person::getName);
                    case "age" -> Comparator.comparingInt(Person::getAge);
                    case "power" -> Comparator.comparingDouble(p -> p.isAwakened() && p.getAetherProfile() != null ? p.getAetherProfile().getDerivedPower() : 0.0);
                    case "wealth" -> Comparator.comparingDouble(Person::getWealth);
                    case "birthyear" -> Comparator.comparingInt(Person::getBirthYear);
                    case "tier" -> Comparator.comparingInt(p -> p.isAwakened() && p.getAetherProfile() != null ? p.getAetherProfile().getPowerTier() : 0);
                    default -> Comparator.comparingInt(p -> p.isAwakened() && p.getAetherProfile() != null ? p.getAetherProfile().getPowerTier() : 0);
                };

                if ("desc".equalsIgnoreCase(sortDir)) {
                    comparator = comparator.reversed();
                }
                filtered.sort(comparator);

                List<PersonSummaryDto> dtos = new ArrayList<>(filtered.size());
                for (Person p : filtered) {
                    dtos.add(SnapshotBuilder.buildPersonSummary(p));
                }
                return dtos;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(results, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<PagedResponse<PersonSummaryDto>> listPeople(
            int page, int size, Integer settlementId, Boolean alive,
            Boolean awakened, String supernaturalClass, Integer tier) {
        return listPeople(page, size, null, settlementId, null, null, alive, awakened, supernaturalClass, tier, "tier", "desc");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDetailDto> getPersonDetail(@PathVariable("id") int id) {
        try {
            PersonDetailDto detail = runner.submitQuery(engine -> {
                Person target = engine.getPerson(id);
                if (target == null) return null;
                return SnapshotBuilder.buildPersonDetail(
                        target,
                        engine.getOrganizationSystem(),
                        engine.getEquipmentCraftingSystem(),
                        engine.getClanHeirloomSystem()
                );
            }).get(3, TimeUnit.SECONDS);

            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/tree")
    public ResponseEntity<GraphDataDto> getPersonTree(@PathVariable("id") int id) {
        try {
            GraphDataDto graph = runner.submitQuery(engine -> {
                Person target = engine.getPerson(id);
                if (target == null) return new GraphDataDto(List.of(), List.of());
                return SnapshotBuilder.buildPersonLineageTree(target, engine);
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
