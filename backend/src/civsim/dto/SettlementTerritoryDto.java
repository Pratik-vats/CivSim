package civsim.dto;

import java.util.List;

/**
 * Phase 3.6 — Settlement territory boundary and sphere of influence.
 */
public record SettlementTerritoryDto(
        int settlementId,
        String settlementName,
        double centerX,
        double centerY,
        double territoryRadius,
        String tier,
        String dominantClan,
        List<Integer> alliedSettlementIds,
        List<Integer> hostileSettlementIds
) {
    public SettlementTerritoryDto {
        alliedSettlementIds = (alliedSettlementIds != null) ? List.copyOf(alliedSettlementIds) : List.of();
        hostileSettlementIds = (hostileSettlementIds != null) ? List.copyOf(hostileSettlementIds) : List.of();
    }
}
