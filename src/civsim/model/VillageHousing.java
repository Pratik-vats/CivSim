package civsim.model;

/**
 * Manages the civic housing infrastructure and capacity limits for a settlement.
 */
public class VillageHousing {
    private int housingCount;

    public VillageHousing(int initialHouses) {
        this.housingCount = Math.max(1, initialHouses);
    }

    public int getHousingCount() {
        return housingCount;
    }

    public void setHousingCount(int housingCount) {
        this.housingCount = Math.max(1, housingCount);
    }

    public void addHousing(int count) {
        if (count > 0) {
            this.housingCount += count;
        }
    }

    public void destroyHousing(int count) {
        if (count > 0) {
            this.housingCount = Math.max(1, this.housingCount - count);
        }
    }

    public int getTotalCapacity(int capacityPerBuilding) {
        return this.housingCount * capacityPerBuilding;
    }

    public int getHomelessCount(int livingPopulation, int capacityPerBuilding) {
        return Math.max(0, livingPopulation - getTotalCapacity(capacityPerBuilding));
    }
}
