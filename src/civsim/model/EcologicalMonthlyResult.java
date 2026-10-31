package civsim.model;

/**
 * Phase 2B.5 — Result record of monthly regional ecological biomass accounting.
 */
public final class EcologicalMonthlyResult {

    private final double previousBiomass;
    private final double monthlyRegeneration;
    private final double availableForConsumption;
    private final double actualConsumption;
    private final double newBiomass;
    private final double unmetDemand;

    public EcologicalMonthlyResult(
            double previousBiomass,
            double monthlyRegeneration,
            double availableForConsumption,
            double actualConsumption,
            double newBiomass,
            double unmetDemand) {
        this.previousBiomass = previousBiomass;
        this.monthlyRegeneration = monthlyRegeneration;
        this.availableForConsumption = availableForConsumption;
        this.actualConsumption = actualConsumption;
        this.newBiomass = newBiomass;
        this.unmetDemand = unmetDemand;
    }

    public double getPreviousBiomass() { return previousBiomass; }
    public double getMonthlyRegeneration() { return monthlyRegeneration; }
    public double getAvailableForConsumption() { return availableForConsumption; }
    public double getActualConsumption() { return actualConsumption; }
    public double getNewBiomass() { return newBiomass; }
    public double getUnmetDemand() { return unmetDemand; }

    public boolean hasStarvation() {
        return unmetDemand > 0.001;
    }

    @Override
    public String toString() {
        return String.format("EcologicalMonthlyResult[Prev:%.1f + Regen:%.1f = Avail:%.1f, Consumed:%.1f, New:%.1f, Unmet:%.1f]",
                previousBiomass, monthlyRegeneration, availableForConsumption, actualConsumption, newBiomass, unmetDemand);
    }
}

