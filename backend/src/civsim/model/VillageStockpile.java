package civsim.model;

import civsim.spatial.Biome;

/**
 * Manages the physical resource stockpiles for a settlement:
 * - Food: Consumed monthly by residents and caravan/military supply trains.
 * - Wood: Timber used for housing construction and winter heating fuel.
 * - Iron/Stone (Ore): Mineral ore used for tool crafting, civic infrastructure, and fortifications.
 * - Tools: Manufactured iron/wood implements that boost worker output and degrade with use.
 */
public class VillageStockpile {
    private double foodStockpile;
    private double woodStockpile;
    private double ironStoneStockpile;
    private double toolsStockpile;

    public VillageStockpile(Biome biome) {
        this.foodStockpile = (biome == Biome.HIGHLAND_MINE) ? 350.0 : ((biome == Biome.FOREST) ? 150.0 : 120.0);
        this.woodStockpile = 60.0;
        this.ironStoneStockpile = (biome == Biome.HIGHLAND_MINE) ? 60.0 : 25.0;
        this.toolsStockpile = (biome == Biome.HIGHLAND_MINE) ? 25.0 : 15.0;
    }

    public double getFoodStockpile() { return foodStockpile; }
    public void addFood(double amount) {
        if (amount > 0) this.foodStockpile += amount;
    }
    public void deductFood(double amount) {
        if (amount > 0) this.foodStockpile = Math.max(0.0, this.foodStockpile - amount);
    }

    public double getWoodStockpile() { return woodStockpile; }
    public void addWood(double amount) {
        if (amount > 0) this.woodStockpile += amount;
    }
    public void deductWood(double amount) {
        if (amount > 0) this.woodStockpile = Math.max(0.0, this.woodStockpile - amount);
    }

    public double getIronStoneStockpile() { return ironStoneStockpile; }
    public void addIronStone(double amount) {
        if (amount > 0) this.ironStoneStockpile += amount;
    }
    public void addOre(double amount) {
        addIronStone(amount);
    }
    public void setStockpiles(double food, double wood, double ironStone, double tools) {
        this.foodStockpile = Math.max(0.0, food);
        this.woodStockpile = Math.max(0.0, wood);
        this.ironStoneStockpile = Math.max(0.0, ironStone);
        this.toolsStockpile = Math.max(0.0, tools);
    }
    public void deductIronStone(double amount) {
        if (amount > 0) this.ironStoneStockpile = Math.max(0.0, this.ironStoneStockpile - amount);
    }

    public double getToolsStockpile() { return toolsStockpile; }
    public void addTools(double amount) {
        if (amount > 0) this.toolsStockpile += amount;
    }
    public void deductTools(double amount) {
        if (amount > 0) this.toolsStockpile = Math.max(0.0, this.toolsStockpile - amount);
    }

    public boolean hasToolsForSoldiers(int soldiers) {
        return soldiers <= 0 || (this.toolsStockpile >= soldiers * 0.40);
    }

    public boolean hasToolsForFarmers(int farmers) {
        return farmers <= 0 || (this.toolsStockpile >= farmers * 0.35);
    }

    public void degradeTools(int activeWorkers) {
        double activeTools = Math.min(this.toolsStockpile, activeWorkers);
        if (activeTools > 0) {
            this.toolsStockpile = Math.max(0.0, this.toolsStockpile - (activeTools * 0.03));
        }
    }

    public double getMonthsOfFoodReserve(int livingPopulation) {
        if (livingPopulation <= 0) return 10.0;
        return this.foodStockpile / (double) livingPopulation;
    }
}
