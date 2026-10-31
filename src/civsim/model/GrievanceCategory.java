package civsim.model;

/**
 * Phase 2C.2 — Supported categories of historical grievances.
 */
public enum GrievanceCategory {
    PERSONAL_MURDER(65.0, "Deliberate killing of an identified person"),
    FAMILY_MURDER(70.0, "Deliberate killing of a family member"),
    CLAN_LEADER_KILLED(75.0, "Targeted killing of an organization leader"),
    FAMILY_MASSACRE(85.0, "Deliberate attack resulting in multiple family deaths"),
    CLAN_MASSACRE(90.0, "Deliberate large-scale killing attributable to an organization"),
    HEIRLOOM_CAPTURED(50.0, "Capture or theft of a registered clan heirloom"),
    EQUIPMENT_STOLEN(25.0, "Deliberate theft of valuable personal or organizational equipment"),
    BETRAYAL(45.0, "Proven violation of an alliance, oath, or formal agreement"),
    TREACHERY(40.0, "Deliberate deception causing serious damage"),
    UNJUST_EXECUTION(70.0, "An execution considered unlawful under applicable governance rules"),
    PEACE_TREATY_VIOLATION(60.0, "Proven breach of an existing peace agreement"),
    OTHER_MAJOR_WRONG(20.0, "Another explicitly supported serious historical offense");

    private final double defaultSeverity;
    private final String description;

    GrievanceCategory(double defaultSeverity, String description) {
        this.defaultSeverity = defaultSeverity;
        this.description = description;
    }

    public double getDefaultSeverity() {
        return defaultSeverity;
    }

    public String getDescription() {
        return description;
    }

    public boolean isViolentOrDeadly() {
        return this == PERSONAL_MURDER || this == FAMILY_MURDER || this == CLAN_LEADER_KILLED
                || this == FAMILY_MASSACRE || this == CLAN_MASSACRE || this == UNJUST_EXECUTION;
    }
}

