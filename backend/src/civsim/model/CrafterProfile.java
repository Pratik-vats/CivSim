package civsim.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Phase 2C.1 -- Artisan proficiency profile tracking specialization skill levels on [0.0, 100.0].
 * Invariant: Proficiency increases ONLY from valid work orders, never from passive aging or combat.
 */
public class CrafterProfile {

    private final Map<CrafterSpecialization, Double> proficiencies = new EnumMap<>(CrafterSpecialization.class);
    private int totalOrdersCompleted = 0;
    private int totalCrafts = 0;
    private int totalEnchants = 0;
    private int totalRepairs = 0;
    private int totalUpgrades = 0;

    public CrafterProfile() {
        for (CrafterSpecialization spec : CrafterSpecialization.values()) {
            proficiencies.put(spec, 5.0); // Baseline apprentice skill
        }
    }

    public double getProficiency(CrafterSpecialization spec) {
        if (spec == null) return 0.0;
        return proficiencies.getOrDefault(spec, 0.0);
    }

    public void setProficiency(CrafterSpecialization spec, double value) {
        if (spec == null) return;
        proficiencies.put(spec, Math.max(0.0, Math.min(100.0, value)));
    }

    /**
     * Advances proficiency with deterministic diminishing returns.
     */
    public double gainProficiency(CrafterSpecialization spec, double baseGain) {
        if (spec == null || baseGain <= 0.0) return 0.0;
        double current = getProficiency(spec);
        double factor = Math.max(0.05, 1.0 - (current / 110.0));
        double actualGain = baseGain * factor;
        double newProf = Math.min(100.0, current + actualGain);
        proficiencies.put(spec, newProf);
        return actualGain;
    }

    public void recordOrderCompletion(CraftingWorkOrderType orderType) {
        totalOrdersCompleted++;
        if (orderType != null) {
            switch (orderType) {
                case NEW_CRAFT:
                    totalCrafts++;
                    break;
                case ENCHANT:
                    totalEnchants++;
                    break;
                case REPAIR:
                    totalRepairs++;
                    break;
                case UPGRADE:
                    totalUpgrades++;
                    break;
            }
        }
    }

    public int getTotalOrdersCompleted() { return totalOrdersCompleted; }
    public void setTotalOrdersCompleted(int v) { this.totalOrdersCompleted = v; }
    public int getTotalCrafts() { return totalCrafts; }
    public void setTotalCrafts(int v) { this.totalCrafts = v; }
    public int getTotalEnchants() { return totalEnchants; }
    public void setTotalEnchants(int v) { this.totalEnchants = v; }
    public int getTotalRepairs() { return totalRepairs; }
    public void setTotalRepairs(int v) { this.totalRepairs = v; }
    public int getTotalUpgrades() { return totalUpgrades; }
    public void setTotalUpgrades(int v) { this.totalUpgrades = v; }

    public double getAverageProficiency() {
        double sum = 0.0;
        for (double p : proficiencies.values()) {
            sum += p;
        }
        return sum / proficiencies.size();
    }
}

