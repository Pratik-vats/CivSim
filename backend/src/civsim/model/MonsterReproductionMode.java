package civsim.model;

/**
 * Phase 2B.5 — Species reproduction mode determining conditions for offspring creation.
 */
public enum MonsterReproductionMode {
    PAIR_BREEDING("Pair Breeding"),
    AETHER_SPAWN("Aether Spawn"),
    ASSEMBLY("Assembly");

    private final String displayName;

    MonsterReproductionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

