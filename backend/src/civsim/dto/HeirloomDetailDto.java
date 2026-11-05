package civsim.dto;

import java.util.List;

/**
 * Full provenance, stats, and historical record for a registered clan heirloom.
 */
public record HeirloomDetailDto(
        String heirloomId,
        String equipmentId,
        String artifactTitle,
        String equipmentType,
        String rarity,
        int tier,
        String craftQuality,
        String condition,
        double durability,
        double maxDurability,
        List<String> enchantments,
        String creatorPersonId,
        String creatorPersonName,
        String owningClanId,
        String owningClanName,
        String currentBearerId,
        String currentBearerName,
        String custodyStatus,
        String successionPolicy,
        int registrationYear,
        int registrationMonth,
        List<BearerRecordDto> pastBearers,
        List<String> battleChronicle
) {
    public HeirloomDetailDto {
        enchantments = (enchantments != null) ? List.copyOf(enchantments) : List.of();
        pastBearers = (pastBearers != null) ? List.copyOf(pastBearers) : List.of();
        battleChronicle = (battleChronicle != null) ? List.copyOf(battleChronicle) : List.of();
    }
}

