package civsim.spatial;

/**
 * Represents the geographical biome of a settlement.
 * Biomes dictate land fertility, natural resource abundance, and production yields,
 * establishing distinct regional specializations that drive inter-settlement trade.
 */
public enum Biome {
    RIVER_VALLEY("River Valley", "RIV", 2.2, 1.8, 0.7, 0.8, 0.4, 1.1),
    FOREST("Forest", "FOR", 1.0, 0.9, 2.5, 2.2, 0.8, 1.0),
    HIGHLAND_MINE("Highland Mine", "HGH", 0.4, 0.5, 0.6, 0.7, 2.6, 1.1),
    COASTAL("Coastal", "CST", 1.2, 1.3, 0.9, 0.9, 0.7, 1.6);

    private final String displayName;
    private final String shortCode;
    private final double arableLandMultiplier;
    private final double cropYieldMultiplier;
    private final double forestReserveMultiplier;
    private final double woodYieldMultiplier;
    private final double oreYieldMultiplier;
    private final double tradeMultiplier;

    Biome(String displayName,
          String shortCode,
          double arableLandMultiplier,
          double cropYieldMultiplier,
          double forestReserveMultiplier,
          double woodYieldMultiplier,
          double oreYieldMultiplier,
          double tradeMultiplier) {
        this.displayName = displayName;
        this.shortCode = shortCode;
        this.arableLandMultiplier = arableLandMultiplier;
        this.cropYieldMultiplier = cropYieldMultiplier;
        this.forestReserveMultiplier = forestReserveMultiplier;
        this.woodYieldMultiplier = woodYieldMultiplier;
        this.oreYieldMultiplier = oreYieldMultiplier;
        this.tradeMultiplier = tradeMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortCode() {
        return shortCode;
    }

    public double getArableLandMultiplier() {
        return arableLandMultiplier;
    }

    public double getCropYieldMultiplier() {
        return cropYieldMultiplier;
    }

    public double getForestReserveMultiplier() {
        return forestReserveMultiplier;
    }

    public double getWoodYieldMultiplier() {
        return woodYieldMultiplier;
    }

    public double getOreYieldMultiplier() {
        return oreYieldMultiplier;
    }

    public double getTradeMultiplier() {
        return tradeMultiplier;
    }

    /**
     * Determines the biome for a given 2D coordinate on a standard [0, 100] world grid.
     */
    public static Biome determineBiomeAt(double x, double y) {
        // Northwest (X <= 42, Y >= 58): Coastal sea & ocean fjords
        if (x <= 42.0 && y >= 58.0) {
            return COASTAL;
        }
        // Northeast (X >= 55, Y >= 55): Rugged mountain highland mines
        if (x >= 55.0 && y >= 55.0) {
            return HIGHLAND_MINE;
        }
        // Southwest (X <= 50, Y <= 50): Fertile river basin & floodplains
        if (x <= 50.0 && y <= 50.0) {
            return RIVER_VALLEY;
        }
        // Central & Southeast: Dense temperate forests
        return FOREST;
    }
}
