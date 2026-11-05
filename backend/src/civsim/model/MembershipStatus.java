package civsim.model;

/**
 * Phase 2B.2 — Status of an individual's membership record in an Organization.
 */
public enum MembershipStatus {
    INVITED("Invited"),
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    LEFT("Left"),
    EXPELLED("Expelled");

    private final String displayName;

    MembershipStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * True if the individual currently has an active, pending, or suspended relation.
     */
    public boolean isCurrentAffiliation() {
        return this == INVITED || this == ACTIVE || this == SUSPENDED;
    }

    public boolean isTerminated() {
        return this == LEFT || this == EXPELLED;
    }
}

