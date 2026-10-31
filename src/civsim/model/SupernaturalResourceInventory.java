package civsim.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Phase 2B.3 -- Dedicated inventory structure for storing supernatural resources,
 * strictly segregated from mundane stockpiles (Food, Wood, Ore, Tools, and Currency).
 */
public class SupernaturalResourceInventory {
    private final Map<SupernaturalResourceType, Double> resources = new EnumMap<>(SupernaturalResourceType.class);

    public SupernaturalResourceInventory() {
        for (SupernaturalResourceType type : SupernaturalResourceType.values()) {
            resources.put(type, 0.0);
        }
    }

    public synchronized double getAmount(SupernaturalResourceType type) {
        if (type == null) return 0.0;
        return resources.getOrDefault(type, 0.0);
    }

    public synchronized void addResource(SupernaturalResourceType type, double amount) {
        if (type == null || amount <= 0.0) return;
        resources.put(type, resources.getOrDefault(type, 0.0) + amount);
    }

    public synchronized double deductResource(SupernaturalResourceType type, double amount) {
        if (type == null || amount <= 0.0) return 0.0;
        double current = resources.getOrDefault(type, 0.0);
        double actualDeducted = Math.min(current, amount);
        resources.put(type, Math.max(0.0, current - actualDeducted));
        return actualDeducted;
    }

    public synchronized double getTotalAmount() {
        double sum = 0.0;
        for (double val : resources.values()) {
            sum += val;
        }
        return sum;
    }

    public synchronized Map<SupernaturalResourceType, Double> getSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(resources));
    }

    public synchronized void clear() {
        for (SupernaturalResourceType type : SupernaturalResourceType.values()) {
            resources.put(type, 0.0);
        }
    }

    @Override
    public synchronized String toString() {
        return String.format("SupernaturalInventory[Crystal:%.1f, Ore:%.1f, Essence:%.1f | Total:%.1f]",
                getAmount(SupernaturalResourceType.RAW_AETHER_CRYSTAL),
                getAmount(SupernaturalResourceType.ANOMALOUS_ORE),
                getAmount(SupernaturalResourceType.CONDENSED_AETHER_ESSENCE),
                getTotalAmount());
    }
}

