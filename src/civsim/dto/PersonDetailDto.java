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
        double mastery,
        double luck,
        double greed,
        double darkResonance,
        double derivedPower,
        Map<String, String> equippedItems,
        Integer familyId,
        String familySurname,
        Integer fatherId,
        String fatherName,
        Integer motherId,
        String motherName,
        Integer spouseId,
        String spouseName,
        List<Integer> childrenIds,
        List<String> organizationMemberships
) {
    public PersonDetailDto {
        equippedItems = (equippedItems != null) ? Map.copyOf(equippedItems) : Map.of();
        childrenIds = (childrenIds != null) ? List.copyOf(childrenIds) : List.of();
        organizationMemberships = (organizationMemberships != null) ? List.copyOf(organizationMemberships) : List.of();
    }
}

