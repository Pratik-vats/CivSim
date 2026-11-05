package civsim.web.controller;

import civsim.dto.FamilyDetailDto;
import civsim.dto.FamilySummaryDto;
import civsim.dto.GraphDataDto;
import civsim.dto.PagedResponse;
import civsim.model.Family;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Family Dynasties and interactive genealogy trees.
 */
@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final SimulationRunner runner;

    public FamilyController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<FamilySummaryDto>> listFamilies(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "lineage") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        try {
            List<FamilySummaryDto> results = runner.submitQuery(engine -> {
                List<Family> allFams = engine.getAllFamilies();
                List<FamilySummaryDto> dtos = new ArrayList<>();
                String query = (search != null) ? search.trim().toLowerCase() : null;

                for (Family fam : allFams) {
                    if (query != null && !query.isEmpty()) {
                        boolean match = fam.getSurname().toLowerCase().contains(query)
                                || String.valueOf(fam.getId()).equals(query)
                                || (fam.getFounder() != null && fam.getFounder().getName().toLowerCase().contains(query));
                        if (!match) continue;
                    }

                    dtos.add(SnapshotBuilder.buildFamilySummary(fam, engine.getAllPeople()));
                }

                Comparator<FamilySummaryDto> comparator = switch (sortBy.toLowerCase()) {
                    case "surname" -> Comparator.comparing(FamilySummaryDto::surname);
                    case "living" -> Comparator.comparingInt(FamilySummaryDto::livingCount);
                    case "founded" -> Comparator.comparingInt(FamilySummaryDto::foundedYear);
                    case "tier" -> Comparator.comparingInt(FamilySummaryDto::highestTier);
                    case "generations" -> Comparator.comparingInt(FamilySummaryDto::generationsCount);
                    case "lineage" -> Comparator.comparingInt(FamilySummaryDto::totalLineageCount);
                    default -> Comparator.comparingInt(FamilySummaryDto::totalLineageCount);
                };

                if ("desc".equalsIgnoreCase(sortDir)) {
                    comparator = comparator.reversed();
                }
                dtos.sort(comparator);

                return dtos;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(results, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyDetailDto> getFamilyDetail(@PathVariable("id") int id) {
        try {
            FamilyDetailDto detail = runner.submitQuery(engine -> {
                Family target = null;
                for (Family fam : engine.getAllFamilies()) {
                    if (fam.getId() == id) {
                        target = fam;
                        break;
                    }
                }
                if (target == null) return null;
                return SnapshotBuilder.buildFamilyDetail(target, engine.getAllPeople(), engine.getOrganizationSystem());
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
    public ResponseEntity<GraphDataDto> getFamilyTree(@PathVariable("id") int id) {
        try {
            GraphDataDto graph = runner.submitQuery(engine -> {
                Family target = null;
                for (Family fam : engine.getAllFamilies()) {
                    if (fam.getId() == id) {
                        target = fam;
                        break;
                    }
                }
                if (target == null) return new GraphDataDto(List.of(), List.of());
                return SnapshotBuilder.buildFamilyTree(target, engine);
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
