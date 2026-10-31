package civsim.model;

/**
 * Phase 2C.1 -- Succession policy governing Clan Heirloom bearer assignment upon bearer death.
 */
public enum HeirloomSuccessionPolicy {
    BLOODLINE_FIRST("Bloodline First"),
    MERIT_FIRST("Merit First"),
    LEADER_DESIGNATED("Leader Designated"),
    COUNCIL_DECISION("Council Decision");

    private final String displayName;

    HeirloomSuccessionPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

