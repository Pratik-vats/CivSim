package civsim.model;

import civsim.spatial.Biome;

/**
 * Phase 2B.5 — Regional ecological model tracking available prey biomass, regeneration, and consumption.
 * Guarantees that monster feeding consumes natural prey biomass without touching human settlement stockpiles.
 */
public class EcologicalRegionState {

    private final String regionId;
    private final Biome biome;
    private double availablePreyBiomass;
    private final double carryingCapacity;
    private final double monthlyRegenerationRate;

    public EcologicalRegionState(String regionId, Biome biome, double carryingCapacity, double monthlyRegenerationRate) {
        this.regionId = regionId;
        this.biome = biome != null ? biome : Biome.FOREST;
        this.carryingCapacity = Math.max(50.0, carryingCapacity);
        this.monthlyRegenerationRate = Math.max(5.0, monthlyRegenerationRate);
        this.availablePreyBiomass = this.carryingCapacity;
    }

    public static EcologicalRegionState createDefault(String regionId, Biome biome) {
        double cap;
        double regen;
        switch (biome != null ? biome : Biome.FOREST) {
            case RIVER_VALLEY:
                cap = 300.0; regen = 45.0; break;
            case FOREST:
                cap = 250.0; regen = 35.0; break;
            case COASTAL:
                cap = 220.0; regen = 30.0; break;
            case HIGHLAND_MINE:
            default:
                cap = 160.0; regen = 20.0; break;
        }
        return new EcologicalRegionState(regionId, biome, cap, regen);
    }

    public synchronized EcologicalMonthlyResult processMonthlyConsumption(double totalDemand) {
        double previousBiomass = this.availablePreyBiomass;
        double availableForConsumption = previousBiomass + this.monthlyRegenerationRate;
        double actualConsumption = Math.min(Math.max(0.0, totalDemand), availableForConsumption);
        this.availablePreyBiomass = Math.max(0.0, Math.min(carryingCapacity, availableForConsumption - actualConsumption));
        double unmetDemand = Math.max(0.0, totalDemand - actualConsumption);
        return new EcologicalMonthlyResult(previousBiomass, this.monthlyRegenerationRate, availableForConsumption, actualConsumption, this.availablePreyBiomass, unmetDemand);
    }

    public synchronized double consumeBiomass(double requestedAmount) {
        if (requestedAmount <= 0.0 || availablePreyBiomass <= 0.0) {
            return 0.0;
        }
        double consumed = Math.min(availablePreyBiomass, requestedAmount);
        availablePreyBiomass -= consumed;
        return consumed;
    }

    public synchronized void regenerate() {
        this.availablePreyBiomass = Math.min(carryingCapacity, availablePreyBiomass + monthlyRegenerationRate);
    }

    public String getRegionId() { return regionId; }
    public Biome getBiome() { return biome; }
    public double getAvailablePreyBiomass() { return availablePreyBiomass; }
    public void setAvailablePreyBiomass(double biomass) { this.availablePreyBiomass = Math.max(0.0, Math.min(carryingCapacity, biomass)); }
    public double getCarryingCapacity() { return carryingCapacity; }
    public double getMonthlyRegenerationRate() { return monthlyRegenerationRate; }

    public boolean isDepleted() {
        return availablePreyBiomass <= 0.001;
    }

    @Override
    public String toString() {
        return String.format("EcologicalRegion[%s (%s): Biomass %.1f/%.1f (+%.1f/mo)]",
                regionId, biome.name(), availablePreyBiomass, carryingCapacity, monthlyRegenerationRate);
    }
}

