package civsim.model;

/**
 * The pathway through which a person awakens their supernatural potential.
 * Each pathway has different age ranges, trigger conditions, and flavor.
 */
public enum AwakeningPathway {
    /** Standard awakening ages 16–25 with age-weighted bell curve probability, modified by Luck */
    NORMAL("Normal Awakening", 16, 25,
            "Natural emergence of Aether potential during young adulthood"),

    /** Early prodigious awakening ages 6–15, extremely rare */
    PRODIGY("Prodigy Awakening", 6, 15,
            "Exceptionally rare early awakening in gifted youth"),

    /** Trauma/adversity-triggered awakening — any age 12+, requires near-death or crisis event */
    CRIMSON_CRUCIBLE("Crimson Crucible", 12, 99,
            "Awakening through trauma, adversity, or near-death experience"),

    /** Late-life epiphany — ages 50–74, based on occupational/leadership tenure */
    TWILIGHT_EPIPHANY("Twilight Epiphany", 50, 74,
            "Rare late-life awakening born from decades of experience and wisdom"),

    /** Dark pathway — failed awakening or extreme trauma causes corruption */
    DARK_AWAKENING("Dark Awakening", 12, 99,
            "Corrupted awakening through excessive Greed or Dark Resonance");

    private final String displayName;
    private final int minAge;
    private final int maxAge;
    private final String description;

    AwakeningPathway(String displayName, int minAge, int maxAge, String description) {
        this.displayName = displayName;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public int getMinAge() { return minAge; }
    public int getMaxAge() { return maxAge; }
    public String getDescription() { return description; }

    public boolean isInAgeRange(int age) {
        return age >= minAge && age <= maxAge;
    }
}
