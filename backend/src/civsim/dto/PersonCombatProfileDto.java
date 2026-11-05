package civsim.dto;

import java.util.List;

/**
 * Combat attributes and derived ratings for an individual character.
 */
public record PersonCombatProfileDto(
        double physicalAttack,
        double physicalDefense,
        double accuracy,
        double evasion,
        double aetherPotency,
        double barrier,
        double currentHealth,
        double maxHealth,
        double currentStamina,
        double maxStamina,
        List<String> injuries
) {
    public PersonCombatProfileDto {
        injuries = (injuries != null) ? List.copyOf(injuries) : List.of();
    }
}

