package civsim.web.controller;

import civsim.dto.EquipmentItemDto;
import civsim.dto.GraphDataDto;
import civsim.dto.HeirloomDetailDto;
import civsim.dto.HeirloomSummaryDto;
import civsim.dto.PagedResponse;
import civsim.model.ClanHeirloom;
import civsim.model.EquipmentItem;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Controller for Clan Heirlooms, Bearer Succession, and Equipment Provenance.
 */
@RestController
@RequestMapping("/api")
public class HeirloomController {

    private final SimulationRunner runner;

    public HeirloomController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/heirlooms")
    public ResponseEntity<PagedResponse<HeirloomSummaryDto>> listHeirlooms(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "clanId", required = false) String clanId,
            @RequestParam(name = "custodyStatus", required = false) String custodyStatus,
            @RequestParam(name = "tier", required = false) Integer tier) {

        try {
            List<HeirloomSummaryDto> results = runner.submitQuery(engine -> {
                List<HeirloomSummaryDto> list = new ArrayList<>();
                if (engine.getClanHeirloomSystem() == null) return list;

                for (ClanHeirloom h : engine.getClanHeirloomSystem().getAllHeirlooms().values()) {
                    if (clanId != null && !h.getOwningClanId().equalsIgnoreCase(clanId)) continue;
                    if (custodyStatus != null && !h.getCustodyStatus().name().equalsIgnoreCase(custodyStatus)) continue;

                    HeirloomSummaryDto dto = SnapshotBuilder.buildHeirloomSummary(
                            h,
                            engine.getEquipmentCraftingSystem(),
                            engine.getOrganizationSystem(),
                            engine
                    );

                    if (tier != null && dto.tier() != tier) continue;
                    list.add(dto);
                }

                list.sort((a, b) -> Integer.compare(b.tier(), a.tier()));
                return list;
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(PagedResponse.of(results, page, size));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/heirlooms/{id}")
    public ResponseEntity<HeirloomDetailDto> getHeirloomDetail(@PathVariable("id") String id) {
        try {
            HeirloomDetailDto detail = runner.submitQuery(engine -> {
                if (engine.getClanHeirloomSystem() == null) return null;
                ClanHeirloom h = engine.getClanHeirloomSystem().getHeirloom(id);
                if (h == null) return null;

                return SnapshotBuilder.buildHeirloomDetail(
                        h,
                        engine.getEquipmentCraftingSystem(),
                        engine.getOrganizationSystem(),
                        engine
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

    @GetMapping("/heirlooms/{id}/succession-graph")
    public ResponseEntity<GraphDataDto> getSuccessionGraph(@PathVariable("id") String id) {
        try {
            GraphDataDto graph = runner.submitQuery(engine -> {
                if (engine.getClanHeirloomSystem() == null) return new GraphDataDto(List.of(), List.of());
                ClanHeirloom h = engine.getClanHeirloomSystem().getHeirloom(id);
                if (h == null) return new GraphDataDto(List.of(), List.of());

                return SnapshotBuilder.buildHeirloomSuccessionGraph(h, engine);
            }).get(3, TimeUnit.SECONDS);

            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/equipment/{id}")
    public ResponseEntity<EquipmentItemDto> getEquipmentDetail(@PathVariable("id") String id) {
        try {
            EquipmentItemDto dto = runner.submitQuery(engine -> {
                if (engine.getEquipmentCraftingSystem() == null) return null;
                EquipmentItem eq = engine.getEquipmentCraftingSystem().getEquipment(id);
                if (eq == null) return null;

                List<String> enchStrs = eq.getEnchantments().stream()
                        .map(en -> en.getFamily() + " (T" + en.getPotencyTier() + " +" + Math.round(en.getPrimaryBonus() * 10.0) / 10.0 + ")")
                        .collect(Collectors.toList());

                return new EquipmentItemDto(
                        eq.getEquipmentId(),
                        eq.getDisplayName(),
                        eq.getSlot() != null ? eq.getSlot().name() : "MAIN_HAND",
                        eq.getType() != null ? eq.getType().name() : "UNKNOWN",
                        eq.getCategory() != null ? eq.getCategory().name() : "WEAPON",
                        eq.getEquipmentRarity() != null ? eq.getEquipmentRarity().name() : "COMMON",
                        eq.getEquipmentTier(),
                        eq.getCondition() != null ? eq.getCondition().name() : "EXCELLENT",
                        Math.round(eq.getDurability() * 10.0) / 10.0,
                        Math.round(eq.getMaximumDurability() * 10.0) / 10.0,
                        eq.getCreatorPersonId(),
                        eq.getCreationTotalMonths() / 12,
                        eq.isRegisteredHeirloom(),
                        eq.getHeirloomId(),
                        enchStrs
                );
            }).get(3, TimeUnit.SECONDS);

            if (dto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
