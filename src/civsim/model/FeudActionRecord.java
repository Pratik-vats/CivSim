package civsim.model;

/**
 * Phase 2C.2 — Immutable record of a decision or action executed in an active feud.
 */
public final class FeudActionRecord {
    private final String actionId;
    private final String feudId;
    private final FeudActionType actionType;
    private final int simulationYear;
    private final int simulationMonth;
    private final String initiatorPersonId;
    private final String initiatorOrgId;
    private final String targetPersonId;
    private final String targetOrgId;
    private final String description;
    private final double costIron;
    private final double costFood;
    private final boolean success;
    private final String details;

    public FeudActionRecord(
            String actionId,
            String feudId,
            FeudActionType actionType,
            int simulationYear,
            int simulationMonth,
            String initiatorPersonId,
            String initiatorOrgId,
            String targetPersonId,
            String targetOrgId,
            String description,
            double costIron,
            double costFood,
            boolean success,
            String details) {
        this.actionId = actionId;
        this.feudId = feudId;
        this.actionType = actionType;
        this.simulationYear = simulationYear;
        this.simulationMonth = simulationMonth;
        this.initiatorPersonId = initiatorPersonId;
        this.initiatorOrgId = initiatorOrgId;
        this.targetPersonId = targetPersonId;
        this.targetOrgId = targetOrgId;
        this.description = description != null ? description : "";
        this.costIron = Math.max(0.0, costIron);
        this.costFood = Math.max(0.0, costFood);
        this.success = success;
        this.details = details != null ? details : "";
    }

    public String getActionId() { return actionId; }
    public String getFeudId() { return feudId; }
    public FeudActionType getActionType() { return actionType; }
    public int getSimulationYear() { return simulationYear; }
    public int getSimulationMonth() { return simulationMonth; }
    public String getInitiatorPersonId() { return initiatorPersonId; }
    public String getInitiatorOrgId() { return initiatorOrgId; }
    public String getTargetPersonId() { return targetPersonId; }
    public String getTargetOrgId() { return targetOrgId; }
    public String getDescription() { return description; }
    public double getCostIron() { return costIron; }
    public double getCostFood() { return costFood; }
    public boolean isSuccess() { return success; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return String.format("FeudAction[%s in %s: %s (Y%d M%d) Success:%b CostIron:%.1f]",
                actionId, feudId, actionType, simulationYear, simulationMonth, success, costIron);
    }
}

