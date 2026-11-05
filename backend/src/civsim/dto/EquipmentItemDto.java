package civsim.dto;

import java.util.List;

/**
 * Inspection DTO for an individual equipment item.
 */
public record EquipmentItemDto(
        String equipmentId,
        String displayName,
        String slot,
        String equipmentType,
        String category,
        String rarity,
        int tier,
        String condition,
        double durability,
        double maxDurability,
        String creatorPersonId,
        int creationYear,
        boolean isRegisteredHeirloom,
        String registeredHeirloomId,
        List<String> enchantments
) {
    public EquipmentItemDto {
        enchantments = (enchantments != null) ? List.copyOf(enchantments) : List.of();
    }
}

