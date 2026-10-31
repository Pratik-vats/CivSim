package civsim.model;

/**
 * Phase 2C.2 — Bounded escalation levels of an active feud.
 */
public enum FeudEscalationLevel {
    LEVEL_0_UNRESOLVED_GRIEVANCE(0, "UNRESOLVED_GRIEVANCE", "Historical claim exists; no active dispute"),
    LEVEL_1_DIPLOMATIC_TENSION(1, "DIPLOMATIC_TENSION", "Public accusations, diplomatic condemnation, demands"),
    LEVEL_2_FORMAL_HOSTILITY(2, "FORMAL_HOSTILITY", "Diplomatic sanctions, broken agreements, formal challenges"),
    LEVEL_3_TARGETED_REPRISAL(3, "TARGETED_REPRISAL", "Limited retaliatory raids or targeted recovery operations"),
    LEVEL_4_ORGANIZED_CLAN_CONFLICT(4, "ORGANIZED_CLAN_CONFLICT", "Coordinated clan-level armed expeditions and clashes"),
    LEVEL_5_INTERGENERATIONAL_VENDETTA(5, "INTERGENERATIONAL_VENDETTA", "Deeply entrenched multi-generational conflict");

    private final int level;
    private final String shortName;
    private final String description;

    FeudEscalationLevel(int level, String shortName, String description) {
        this.level = level;
        this.shortName = shortName;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public static FeudEscalationLevel fromLevel(int level) {
        if (level <= 0) return LEVEL_0_UNRESOLVED_GRIEVANCE;
        if (level == 1) return LEVEL_1_DIPLOMATIC_TENSION;
        if (level == 2) return LEVEL_2_FORMAL_HOSTILITY;
        if (level == 3) return LEVEL_3_TARGETED_REPRISAL;
        if (level == 4) return LEVEL_4_ORGANIZED_CLAN_CONFLICT;
        return LEVEL_5_INTERGENERATIONAL_VENDETTA;
    }
}

