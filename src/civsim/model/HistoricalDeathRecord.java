package civsim.model;

/**
 * Stage 2A.5 — Immutable historical death record.
 * Preserves a permanent, heap-safe snapshot of a citizen's life and death:
 * actual age at death, awakened status, class, aspect, final tier,
 * final longevity offset, natural ceiling at death, and cause category.
 */
public final class HistoricalDeathRecord {
    private final int personId;
    private final String fullName;
    private final int deathYear;
    private final int ageAtDeath;
    private final boolean awakened;
    private final SupernaturalClass supernaturalClass; // null if mortal
    private final boolean darkAspect;
    private final int finalTier;                      // 0 if mortal
    private final int finalLongevityOffset;           // 0 if mortal
    private final int effectiveNaturalLifespanCeilingAtDeath;
    private final String causeOfDeath;
    private final boolean naturalDeath;
    private final int breakthroughAttempts;
    private final int breakthroughSuccesses;
    private final int breakthroughFailures;

    public HistoricalDeathRecord(
            int personId,
            String fullName,
            int deathYear,
            int ageAtDeath,
            boolean awakened,
            SupernaturalClass supernaturalClass,
            boolean darkAspect,
            int finalTier,
            int finalLongevityOffset,
            int effectiveNaturalLifespanCeilingAtDeath,
            String causeOfDeath,
            boolean naturalDeath,
            int breakthroughAttempts,
            int breakthroughSuccesses,
            int breakthroughFailures) {
        this.personId = personId;
        this.fullName = fullName;
        this.deathYear = deathYear;
        this.ageAtDeath = ageAtDeath;
        this.awakened = awakened;
        this.supernaturalClass = supernaturalClass;
        this.darkAspect = darkAspect;
        this.finalTier = finalTier;
        this.finalLongevityOffset = finalLongevityOffset;
        this.effectiveNaturalLifespanCeilingAtDeath = effectiveNaturalLifespanCeilingAtDeath;
        this.causeOfDeath = causeOfDeath;
        this.naturalDeath = naturalDeath;
        this.breakthroughAttempts = breakthroughAttempts;
        this.breakthroughSuccesses = breakthroughSuccesses;
        this.breakthroughFailures = breakthroughFailures;
    }

    public HistoricalDeathRecord(
            int personId,
            String fullName,
            int deathYear,
            int ageAtDeath,
            boolean awakened,
            SupernaturalClass supernaturalClass,
            boolean darkAspect,
            int finalTier,
            int finalLongevityOffset,
            int effectiveNaturalLifespanCeilingAtDeath,
            String causeOfDeath,
            boolean naturalDeath) {
        this(personId, fullName, deathYear, ageAtDeath, awakened, supernaturalClass, darkAspect,
                finalTier, finalLongevityOffset, effectiveNaturalLifespanCeilingAtDeath,
                causeOfDeath, naturalDeath, 0, 0, 0);
    }

    public int getPersonId() { return personId; }
    public String getFullName() { return fullName; }
    public int getDeathYear() { return deathYear; }
    public int getAgeAtDeath() { return ageAtDeath; }
    public boolean isAwakened() { return awakened; }
    public SupernaturalClass getSupernaturalClass() { return supernaturalClass; }
    public boolean isDarkAspect() { return darkAspect; }
    public int getFinalTier() { return finalTier; }
    public int getFinalLongevityOffset() { return finalLongevityOffset; }
    public int getEffectiveNaturalLifespanCeilingAtDeath() { return effectiveNaturalLifespanCeilingAtDeath; }
    public String getCauseOfDeath() { return causeOfDeath; }
    public boolean isNaturalDeath() { return naturalDeath; }
    public int getBreakthroughAttempts() { return breakthroughAttempts; }
    public int getBreakthroughSuccesses() { return breakthroughSuccesses; }
    public int getBreakthroughFailures() { return breakthroughFailures; }

    @Override
    public String toString() {
        return String.format("DeathRecord[%s (id:%d), died Y%d age %d, %s%s, tier=T%d (offset=+%dy, ceil=%d), breakthroughs(att:%d suc:%d fail:%d), cause='%s' (natural=%b)]",
                fullName, personId, deathYear, ageAtDeath,
                awakened ? (supernaturalClass != null ? supernaturalClass.getDisplayName() : "Awakened") : "Mortal",
                darkAspect ? " [DARK]" : "",
                finalTier, finalLongevityOffset, effectiveNaturalLifespanCeilingAtDeath,
                breakthroughAttempts, breakthroughSuccesses, breakthroughFailures,
                causeOfDeath, naturalDeath);
    }
}
