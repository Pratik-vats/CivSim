package civsim.model;

/**
 * Phase 2B.1 — Severity levels and mechanical penalties for persistent combat injuries.
 */
public enum CombatInjurySeverity {
    /** Minor combat wounds: 1 simulated month, -5.0 physical attributes */
    MINOR_INJURY("Minor Injury", 1, 5.0, 0.0, false),

    /** Moderate combat wounds (Wounded): 2 simulated months, -12.0 physical attributes */
    MODERATE_INJURY("Wounded", 2, 12.0, 0.0, false),

    /** Severe trauma: 3 simulated months, -22.0 physical & Aether attributes, blocks training */
    SEVERE_INJURY("Severe Injury", 3, 22.0, 22.0, true);

    private final String displayName;
    private final int durationMonths;
    private final double physicalPenalty;
    private final double aetherPenalty;
    private final boolean blocksTraining;

    CombatInjurySeverity(String displayName, int durationMonths,
                         double physicalPenalty, double aetherPenalty,
                         boolean blocksTraining) {
        this.displayName = displayName;
        this.durationMonths = durationMonths;
        this.physicalPenalty = physicalPenalty;
        this.aetherPenalty = aetherPenalty;
        this.blocksTraining = blocksTraining;
    }

    public String getDisplayName() { return displayName; }
    public int getDurationMonths() { return durationMonths; }
    public double getPhysicalPenalty() { return physicalPenalty; }
    public double getAetherPenalty() { return aetherPenalty; }
    public boolean blocksTraining() { return blocksTraining; }
}
