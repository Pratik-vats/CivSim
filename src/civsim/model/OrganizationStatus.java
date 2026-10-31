package civsim.model;

/**
 * Phase 2B.2 — Operational lifecycle status of an Organization.
 */
public enum OrganizationStatus {
    FORMING("Forming"),
    ACTIVE("Active"),
    DORMANT("Dormant"),
    DISSOLVED("Dissolved");

    private final String displayName;

    OrganizationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canRecruit() {
        return this == FORMING || this == ACTIVE;
    }

    public boolean isPermanentlyClosed() {
        return this == DISSOLVED;
    }
}

