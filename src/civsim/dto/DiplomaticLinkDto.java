package civsim.dto;

/**
 * Phase 3.6 — Bilateral diplomatic relationship or commercial trade flow between settlements.
 */
public record DiplomaticLinkDto(
        int originSettlementId,
        String originName,
        double originX,
        double originY,
        int targetSettlementId,
        String targetName,
        double targetX,
        double targetY,
        String relationType,
        double tradeVolume,
        String primaryTradedGood
) {}

