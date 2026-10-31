package civsim;

/**
 * Four-season agricultural and climate cycle:
 * - Spring (Months 0–2): Planting season. 0.2x food yield (foraging / early livestock).
 * - Summer (Months 3–5): Growing season. 0.5x food yield.
 * - Autumn (Months 6–8): Harvest season. 3.0x massive crop yield spike stored in granaries.
 * - Winter (Months 9–11): Frost season. 0.0x crop yield; citizens consume 1.5x food and require firewood to survive.
 */
public enum Season {
    SPRING("Spring", 0.2, 1.0, 0.0, "Planting season: foraging/livestock only (0.2x yield)"),
    SUMMER("Summer", 0.5, 1.0, 0.0, "Growing season: moderate crop yield (0.5x yield)"),
    AUTUMN("Autumn", 3.0, 1.0, 0.0, "Harvest season: massive crop yield spike (3.0x yield)"),
    WINTER("Winter", 0.0, 1.5, 0.5, "Frost season: 0.0x crops, 1.5x food & 0.5 firewood/person required");

    private final String displayName;
    private final double cropYieldMultiplier;
    private final double foodConsumptionMultiplier;
    private final double firewoodPerPersonPerMonth;
    private final String description;

    Season(String displayName, double cropYieldMultiplier, double foodConsumptionMultiplier, double firewoodPerPersonPerMonth, String description) {
        this.displayName = displayName;
        this.cropYieldMultiplier = cropYieldMultiplier;
        this.foodConsumptionMultiplier = foodConsumptionMultiplier;
        this.firewoodPerPersonPerMonth = firewoodPerPersonPerMonth;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public double getCropYieldMultiplier() { return cropYieldMultiplier; }
    public double getFoodConsumptionMultiplier() { return foodConsumptionMultiplier; }
    public double getFirewoodPerPersonPerMonth() { return firewoodPerPersonPerMonth; }
    public String getDescription() { return description; }

    public static Season forMonth(int month) {
        int m = ((month % 12) + 12) % 12;
        if (m <= 2) return SPRING;
        if (m <= 5) return SUMMER;
        if (m <= 8) return AUTUMN;
        return WINTER;
    }
}
