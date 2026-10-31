package civsim.model;

/**
 * Phase 2B.4 -- Mathematical snapshot of the 4-bucket resource conservation law for an anomaly or global world:
 * InitializedQuantity == RemainingInDeposit + CarriedByActiveExpeditions + DeliveredToInventories + ExplicitlyAbandonedOrMutated.
 */
public final class ResourceConservationSnapshot {
    private final String anomalyId;
    private final double initializedQuantity;
    private final double remainingInDeposit;
    private final double carriedByActiveExpeditions;
    private final double deliveredToInventories;
    private final double explicitlyAbandonedOrMutated;

    public ResourceConservationSnapshot(
            String anomalyId,
            double initializedQuantity,
            double remainingInDeposit,
            double carriedByActiveExpeditions,
            double deliveredToInventories,
            double explicitlyAbandonedOrMutated) {
        this.anomalyId = anomalyId;
        this.initializedQuantity = initializedQuantity;
        this.remainingInDeposit = remainingInDeposit;
        this.carriedByActiveExpeditions = carriedByActiveExpeditions;
        this.deliveredToInventories = deliveredToInventories;
        this.explicitlyAbandonedOrMutated = explicitlyAbandonedOrMutated;
    }

    public String getAnomalyId() { return anomalyId; }
    public double getInitializedQuantity() { return initializedQuantity; }
    public double getRemainingInDeposit() { return remainingInDeposit; }
    public double getCarriedByActiveExpeditions() { return carriedByActiveExpeditions; }
    public double getDeliveredToInventories() { return deliveredToInventories; }
    public double getExplicitlyAbandonedOrMutated() { return explicitlyAbandonedOrMutated; }

    public double getTotalAccountedFor() {
        return remainingInDeposit + carriedByActiveExpeditions + deliveredToInventories + explicitlyAbandonedOrMutated;
    }

    public double getDiscrepancy() {
        return Math.abs(initializedQuantity - getTotalAccountedFor());
    }

    public boolean isConserved(double tolerance) {
        return getDiscrepancy() <= tolerance;
    }

    public boolean isConserved() {
        return isConserved(0.001);
    }

    @Override
    public String toString() {
        return String.format("Conservation[%s | Init:%.2f == Rem:%.2f + Carried:%.2f + Delivered:%.2f + Mutated:%.2f (Diff:%.4f)]",
                anomalyId, initializedQuantity, remainingInDeposit, carriedByActiveExpeditions,
                deliveredToInventories, explicitlyAbandonedOrMutated, getDiscrepancy());
    }
}
