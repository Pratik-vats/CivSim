package civsim.model;

/**
 * Phase 2B.4 -- Strategic claim objective held by an Expedition or Coalition at a World Anomaly site.
 */
public enum ClaimObjective {
    EXCLUSIVE_CLAIM("Exclusive Claim"),
    JOINT_EXTRACTION("Joint Extraction"),
    SCOUTING_ONLY("Scouting Only"),
    DEFEND_CONTROL("Defend Control"),
    CONTESTED_CAPTURE("Contested Capture");

    private final String displayName;

    ClaimObjective(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isScouting() {
        return this == SCOUTING_ONLY;
    }

    public boolean isJointExtraction() {
        return this == JOINT_EXTRACTION;
    }

    public boolean isAggressive() {
        return this == EXCLUSIVE_CLAIM || this == CONTESTED_CAPTURE;
    }

    public boolean isDefending() {
        return this == DEFEND_CONTROL;
    }
}
