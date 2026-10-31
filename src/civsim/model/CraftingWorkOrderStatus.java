package civsim.model;

/**
 * Phase 2C.1 -- Explicit lifecycle states of a crafting work order.
 * Follows: CREATED -> VALIDATING -> MATERIALS_RESERVED -> IN_PROGRESS -> COMPLETED / FAILED / CANCELLED.
 */
public enum CraftingWorkOrderStatus {
    CREATED("Created"),
    VALIDATING("Validating"),
    MATERIALS_RESERVED("Materials Reserved"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    CraftingWorkOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == MATERIALS_RESERVED || this == IN_PROGRESS;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}

