package civsim.model;

/**
 * Stage 2A.6 — Supernatural aether aspect (Aetheric or Dark).
 */
public enum AetherAspect {
    AETHERIC("Aetheric"),
    DARK("Dark");

    private final String displayName;

    AetherAspect(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDark() {
        return this == DARK;
    }

    public static AetherAspect fromDark(boolean isDark) {
        return isDark ? DARK : AETHERIC;
    }
}
