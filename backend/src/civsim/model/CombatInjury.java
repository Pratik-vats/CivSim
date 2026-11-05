package civsim.model;

/**
 * Phase 2B.1 — Immutable record representing an active, persistent combat injury.
 * Persists across battles and the simulation timeline until recoveryTotalMonths is reached.
 */
public final class CombatInjury {
    private final String injuryId;
    private final CombatInjurySeverity severity;
    private final int inflictedYear;
    private final int inflictedMonth;
    private final long recoveryTotalMonths;
    private final String description;

    public CombatInjury(String injuryId, CombatInjurySeverity severity,
                        int inflictedYear, int inflictedMonth,
                        long recoveryTotalMonths, String description) {
        this.injuryId = injuryId;
        this.severity = severity != null ? severity : CombatInjurySeverity.MINOR_INJURY;
        this.inflictedYear = inflictedYear;
        this.inflictedMonth = inflictedMonth;
        this.recoveryTotalMonths = recoveryTotalMonths;
        this.description = description != null ? description : "Combat injury";
    }

    public String getInjuryId() { return injuryId; }
    public CombatInjurySeverity getSeverity() { return severity; }
    public int getInflictedYear() { return inflictedYear; }
    public int getInflictedMonth() { return inflictedMonth; }
    public long getRecoveryTotalMonths() { return recoveryTotalMonths; }
    public String getDescription() { return description; }

    public boolean isExpired(long currentTotalMonths) {
        return currentTotalMonths >= recoveryTotalMonths;
    }

    public double getAttributePenalty(AttributeType attr) {
        if (attr == null) return 0.0;
        switch (attr) {
            case STRENGTH:
            case AGILITY:
            case ENDURANCE:
                return severity.getPhysicalPenalty();
            case VITALITY:
                return (severity == CombatInjurySeverity.MODERATE_INJURY || severity == CombatInjurySeverity.SEVERE_INJURY)
                        ? severity.getPhysicalPenalty() : 0.0;
            case AETHER_CAPACITY:
            case AETHER_CONTROL:
                return severity.getAetherPenalty();
            default:
                return 0.0;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (inflicted Y%d M%d, recovers M%d): -%.0f phys",
                severity.getDisplayName(), inflictedYear, inflictedMonth, recoveryTotalMonths, severity.getPhysicalPenalty());
    }
}
