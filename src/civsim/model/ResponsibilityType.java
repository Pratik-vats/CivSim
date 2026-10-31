package civsim.model;

/**
 * Phase 2C.2 — Classification of responsibility for a historical grievance.
 */
public enum ResponsibilityType {
    DIRECT_PERPETRATOR("The person who physically committed the act"),
    COMMAND_RESPONSIBILITY("A leader who authorized or knowingly ordered the act"),
    INSTITUTIONAL("An organization that formally authorized, sponsored, or executed the act"),
    ACCOMPLICE("A participant whose documented actions materially contributed to the event");

    private final String description;

    ResponsibilityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

