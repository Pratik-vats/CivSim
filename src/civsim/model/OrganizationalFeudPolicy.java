package civsim.model;

/**
 * Phase 2C.2 — Organizational policy governing clan/faction participation in feuds and inherited grievances.
 */
public enum OrganizationalFeudPolicy {
    FORGIVE_AND_RECONCILE(false, false, true, "Prefers reconciliation, peace offerings, and mutual forgiveness"),
    SEEK_JUSTICE(false, true, true, "Pursues formal justice, arbitration, and lawful restitution"),
    DEMAND_COMPENSATION(false, true, true, "Demands economic restitution or return of property before peace"),
    DEFENSIVE_ONLY(false, false, false, "Will not initiate reprisal; mobilizes only if settlement/members attacked"),
    TARGETED_RETALIATION(true, false, false, "Authorizes precise armed retaliation against confirmed culprits"),
    HONOR_BOUND_REPRISAL(true, false, false, "Obligated by clan code to avenge fallen leaders and stolen heirlooms"),
    NO_INTERVENTION(false, false, false, "Treats grievances as private disputes; clan refuses official involvement");

    private final boolean permitsHostileRetaliation;
    private final boolean prefersDiplomacy;
    private final boolean acceptsCompensation;
    private final String description;

    OrganizationalFeudPolicy(
            boolean permitsHostileRetaliation,
            boolean prefersDiplomacy,
            boolean acceptsCompensation,
            String description) {
        this.permitsHostileRetaliation = permitsHostileRetaliation;
        this.prefersDiplomacy = prefersDiplomacy;
        this.acceptsCompensation = acceptsCompensation;
        this.description = description;
    }

    public boolean permitsHostileRetaliation() { return permitsHostileRetaliation; }
    public boolean prefersDiplomacy() { return prefersDiplomacy; }
    public boolean acceptsCompensation() { return acceptsCompensation; }
    public String getDescription() { return description; }

    public static OrganizationalFeudPolicy createDefault(OrganizationAlignment alignment, OrganizationType type) {
        if (alignment == OrganizationAlignment.DARK_ALIGNED || type == OrganizationType.DARK_COVEN) {
            return TARGETED_RETALIATION;
        } else if (alignment == OrganizationAlignment.AETHERIC_ALIGNED || type == OrganizationType.AWAKENED_ORDER) {
            return SEEK_JUSTICE;
        } else if (type == OrganizationType.MERCENARY_COMPANY) {
            return DEMAND_COMPENSATION;
        } else if (type == OrganizationType.RELIGIOUS_OR_PHILOSOPHICAL_ORDER) {
            return FORGIVE_AND_RECONCILE;
        } else {
            return DEMAND_COMPENSATION;
        }
    }
}
