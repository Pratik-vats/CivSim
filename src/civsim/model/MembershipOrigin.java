package civsim.model;

/**
 * Phase 2B.2 — The originating circumstances under which a person joined an Organization.
 */
public enum MembershipOrigin {
    FOUNDING("Founding"),
    INVITATION("Invitation"),
    OPEN_JOIN("Open Join"),
    BIRTH_TRADITION("Birth Tradition"),
    APPOINTMENT("Appointment");

    private final String displayName;

    MembershipOrigin(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

