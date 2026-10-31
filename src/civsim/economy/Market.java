package civsim.economy;

public class Market {
    private double foodPrice;
    private double annualTradeVolume;
    private double totalTradeVolume;

    public Market(double initialFoodPrice) {
        this.foodPrice = initialFoodPrice;
        this.annualTradeVolume = 0.0;
        this.totalTradeVolume = 0.0;
    }

    public void updateMarketPrices(double foodStockpile, int population) {
        if (population <= 0) return;
        double ratio = foodStockpile / (population * 12.0);
        if (ratio < 0.5) {
            this.foodPrice *= 1.10;
        } else if (ratio > 2.0) {
            this.foodPrice *= 0.90;
        }
        this.foodPrice = Math.max(0.5, Math.min(10.0, this.foodPrice));
    }

    public double getFoodPrice() { return foodPrice; }
    public double getAnnualTradeVolume() { return annualTradeVolume; }
    public double getTotalTradeVolume() { return totalTradeVolume; }

    public void addTradeVolume(double volume) {
        if (volume > 0) {
            this.annualTradeVolume += volume;
            this.totalTradeVolume += volume;
        }
    }

    public void resetAnnualTradeVolume() {
        this.annualTradeVolume = 0.0;
    }
}

