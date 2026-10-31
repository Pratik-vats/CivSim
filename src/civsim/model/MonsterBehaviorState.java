package civsim.model;

/**
 * Phase 2B.5 — Behavioral state of a living Monster entity.
 */
public enum MonsterBehaviorState {
    IDLE("Idle"),
    PATROLLING("Patrolling"),
    HUNTING("Hunting"),
    FEEDING("Feeding"),
    RESTING("Resting"),
    MIGRATING("Migrating"),
    DEFENDING_LAIR("Defending Lair"),
    ATTACKING("Attacking"),
    RETREATING("Retreating"),
    DORMANT("Dormant"),
    DEAD("Dead");

    private final String displayName;

    MonsterBehaviorState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActiveCombatant() {
        return this != DEAD && this != DORMANT && this != RETREATING;
    }

    public boolean isTerminal() {
        return this == DEAD;
    }
}

