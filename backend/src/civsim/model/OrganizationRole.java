package civsim.model;

/**
 * Phase 2B.2 — Internal hierarchy roles and authority within an Organization.
 */
public enum OrganizationRole {
    FOUNDER("Founder", 95.0),
    LEADER("Leader", 100.0),
    ELDER("Elder", 75.0),
    OFFICER("Officer", 65.0),
    CHAMPION("Champion", 60.0),
    MEMBER("Member", 30.0),
    RECRUIT("Recruit", 10.0);

    private final String displayName;
    private final double rankScore;

    OrganizationRole(String displayName, double rankScore) {
        this.displayName = displayName;
        this.rankScore = rankScore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getRankScore() {
        return rankScore;
    }

    public boolean canLead() {
        return this == LEADER || this == FOUNDER;
    }

    public boolean canAppointAndPolicy() {
        return this == LEADER || this == FOUNDER;
    }

    public boolean canInvite() {
        return this == LEADER || this == FOUNDER || this == ELDER || this == OFFICER;
    }

    public boolean isProbationary() {
        return this == RECRUIT;
    }

    public boolean isEligibleForSuccession() {
        return this != RECRUIT;
    }
}

