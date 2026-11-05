package civsim.dto;

import java.util.List;
import java.util.Map;

/**
 * Full inspection details for a person (living or historical deceased).
 */
public record PersonDetailDto(
        int id,
        String name,
        String gender,
        int age,
        boolean alive,
        int birthYear,
        Integer deathYear,
        Integer ageAtDeath,
        String causeOfDeath,
        Integer villageId,
        String villageName,
        String occupation,
        double wealth,
        double bankBalance,
        double loanDebt,
        boolean isAwakened,
        String supernaturalClass,
        String aspect,
        String rarity,
        int tier,
        int maxTier,
        double mastery,
        double luck,
        double greed,
        double darkResonance,
        double derivedPower,
        int breakthroughAttempts,
        int breakthroughSuccesses,
        Map<String, String> equippedItems,
        Map<String, EquipmentItemDto> equipmentDetails,
        PersonCombatProfileDto combatProfile,
        Map<String, AttributeScoresDto> attributes,
        Integer familyId,
        String familySurname,
        Integer fatherId,
        String fatherName,
        Integer motherId,
        String motherName,
        Integer spouseId,
        String spouseName,
        List<Integer> childrenIds,
        List<String> organizationMemberships,
        List<String> achievements
) {
    public PersonDetailDto {
        equippedItems = (equippedItems != null) ? Map.copyOf(equippedItems) : Map.of();
        equipmentDetails = (equipmentDetails != null) ? Map.copyOf(equipmentDetails) : Map.of();
        attributes = (attributes != null) ? Map.copyOf(attributes) : Map.of();
        childrenIds = (childrenIds != null) ? List.copyOf(childrenIds) : List.of();
        organizationMemberships = (organizationMemberships != null) ? List.copyOf(organizationMemberships) : List.of();
        achievements = (achievements != null) ? List.copyOf(achievements) : List.of();
    }
}
