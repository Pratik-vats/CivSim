package civsim.web.controller;

import civsim.dto.OrganizationDetailDto;
import civsim.dto.OrganizationSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.Organization;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Read-only organization and clan inspection endpoints.
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final SimulationRunner runner;

    public OrganizationController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrganizationSummaryDto>> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<OrganizationSummaryDto> list = runner.submitQuery(engine -> {
                List<OrganizationSummaryDto> dtos = new ArrayList<>();
                for (Organization org : engine.getOrganizationSystem().getAllOrganizations()) {
                    dtos.add(SnapshotBuilder.buildOrganizationSummary(org));
                }
                return dtos;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(list, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDetailDto> getOrganizationDetail(@PathVariable String id) {
        try {
            OrganizationDetailDto detail = runner.submitQuery(engine -> {
                Organization target = engine.getOrganizationSystem().getOrganization(id);
                if (target == null) return null;
                return SnapshotBuilder.buildOrganizationDetail(target);
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

