package civsim.model;

/**
 * Phase 2B.2 — Distinct categories of social and political organizations.
 */
public enum OrganizationType {
    CLAN("Clan", SuccessionPolicy.LINEAGE_AND_SENIORITY, false),
    AWAKENED_ORDER("Awakened Order", SuccessionPolicy.MASTERY_AND_TIER_MERIT, true),
    DARK_COVEN("Dark Coven", SuccessionPolicy.AMBITION_AND_POWER, true),
    MERCENARY_COMPANY("Mercenary Company", SuccessionPolicy.COMBAT_PROWESS_AND_EXPERIENCE, false),
    SCHOLARLY_ORDER("Scholarly Order", SuccessionPolicy.COUNCIL_OR_ELDER_ELECTION, false),
    SETTLEMENT_GUARD("Settlement Guard", SuccessionPolicy.SETTLEMENT_APPOINTED, false),
    RELIGIOUS_OR_PHILOSOPHICAL_ORDER("Religious or Philosophical Order", SuccessionPolicy.COUNCIL_OR_ELDER_ELECTION, false);

    private final String displayName;
    private final SuccessionPolicy defaultSuccessionPolicy;
    private final boolean supernatural;

    OrganizationType(String displayName, SuccessionPolicy defaultSuccessionPolicy, boolean supernatural) {
        this.displayName = displayName;
        this.defaultSuccessionPolicy = defaultSuccessionPolicy;
        this.supernatural = supernatural;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SuccessionPolicy getDefaultSuccessionPolicy() {
        return defaultSuccessionPolicy;
    }

    /**
     * Whether this organization type requires the Supernatural Era (Year 50+) and fantasy mode enabled.
     */
    public boolean isSupernatural() {
        return supernatural;
    }

    /**
     * Whether this organization type can be founded in the purely mundane era (Years 0-49).
     */
    public boolean isMundaneAllowed() {
        return !supernatural;
    }
}

