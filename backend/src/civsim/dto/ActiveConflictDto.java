package civsim.dto;

/**
 * Phase 3.6 — Visual indicator marker for an active battle, siege, or raid on the world map.
 */
public record ActiveConflictDto(
        String conflictId,
        String name,
        double x,
        double y,
        String conflictType,
        int totalCombatants,
        int year,
        int month,
        String outcome
) {}

