package civsim.model;

public enum SettlementTier {
    HAMLET("Hamlet", 10, 25, 400.0, 0.0),
    VILLAGE("Village", 30, 65, 1000.0, 15.0),
    TOWN("Town", 75, 180, 2500.0, 40.0),
    CITY("City", 200, 550, 8000.0, 100.0),
    MEGACITY("Megacity", Integer.MAX_VALUE, 3000, 30000.0, 250.0);

    private final String displayName;
    private final int baseInfrastructureSlots;
    private final int baseArableLandSlots;
    private final double baseForestReserves;
    private final double stoneRequiredForUpgrade;

    SettlementTier(
            String displayName,
            int baseInfrastructureSlots,
            int baseArableLandSlots,
            double baseForestReserves,
            double stoneRequiredForUpgrade) {
        this.displayName = displayName;
        this.baseInfrastructureSlots = baseInfrastructureSlots;
        this.baseArableLandSlots = baseArableLandSlots;
        this.baseForestReserves = baseForestReserves;
        this.stoneRequiredForUpgrade = stoneRequiredForUpgrade;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseInfrastructureSlots() {
        return baseInfrastructureSlots;
    }

    public int getBaseArableLandSlots() {
        return baseArableLandSlots;
    }

    public double getBaseForestReserves() {
        return baseForestReserves;
    }

    public double getStoneRequiredForUpgrade() {
        return stoneRequiredForUpgrade;
    }

    public boolean hasHousingCap() {
        return this != MEGACITY;
    }

    public static SettlementTier forPopulation(
            int livingPop,
            int villageThreshold,
            int townThreshold,
            int cityThreshold) {
        return forPopulation(livingPop, villageThreshold, townThreshold, cityThreshold, cityThreshold * 3);
    }

    public static SettlementTier forPopulation(
            int livingPop,
            int villageThreshold,
            int townThreshold,
            int cityThreshold,
            int megacityThreshold) {
        if (livingPop >= megacityThreshold) {
            return MEGACITY;
        } else if (livingPop >= cityThreshold) {
            return CITY;
        } else if (livingPop >= townThreshold) {
            return TOWN;
        } else if (livingPop >= villageThreshold) {
            return VILLAGE;
        } else {
            return HAMLET;
        }
    }
}
