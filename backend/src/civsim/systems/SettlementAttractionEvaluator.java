package civsim.systems;

import civsim.model.*;
import java.util.*;

/**
 * Phase 2B.5 — Evaluates candidate settlements as targets for an active MonsterTide invasion
 * based on spatial proximity, aetheric signature, food/wealth attraction, regional hunger, and defense deterrence.
 */
public final class SettlementAttractionEvaluator {

    public static final double MAX_RAID_DISTANCE = 85.0;

    private SettlementAttractionEvaluator() {}

    public static double computeAttractionScore(
            MonsterLair lair,
            Village village,
            EcologicalRegionState regionState,
            List<Person> allPeople) {

        if (lair == null || village == null) return -1000.0;

        double dx = lair.getPosX() - village.getX();
        double dy = lair.getPosY() - village.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > MAX_RAID_DISTANCE) {
            return -1000.0; // Out of raid range
        }

        // 1. Proximity Score (0 to 100)
        double proximityScore = Math.max(0.0, (MAX_RAID_DISTANCE - dist) / MAX_RAID_DISTANCE * 100.0);

        // 2. Aetheric Signature Score
        double aetherScore = 0.0;
        int awakenedCount = 0;
        if (allPeople != null) {
            for (Person p : allPeople) {
                if (p != null && p.isAlive() && village.equals(p.getVillage()) && p.isAwakened()) {
                    awakenedCount++;
                    int tier = (p.getAetherProfile() != null) ? p.getAetherProfile().getPowerTier() : 1;
                    aetherScore += (tier * 12.0);
                }
            }
        }
        SupernaturalResourceInventory supInv = village.getSupernaturalInventory();
        if (supInv != null) {
            aetherScore += (supInv.getTotalAmount() * 0.50);
        }
        aetherScore = Math.min(100.0, aetherScore);

        // 3. Wealth and Food Score (0 to 100)
        double foodStock = village.getFoodStockpile();
        double wealth = village.getTreasury();
        double wealthFoodScore = Math.min(100.0, (foodStock / 10.0) + (wealth * 0.20));

        // 4. Lair Hunger Pressure (0 to 100)
        double hungerScore = 0.0;
        if (regionState != null && regionState.getCarryingCapacity() > 0.0) {
            double deficit = regionState.getCarryingCapacity() - regionState.getAvailablePreyBiomass();
            hungerScore = Math.max(0.0, Math.min(100.0, (deficit / regionState.getCarryingCapacity()) * 100.0));
        }

        // 5. Deterrence Score (Fortifications & Defenders)
        double deterrence = (village.getFortificationLevel() * 15.0) + (village.getSoldierCount() * 6.0) + (awakenedCount * 12.0);

        // Weighted Combination
        double totalScore = (0.35 * proximityScore)
                + (0.30 * aetherScore)
                + (0.15 * wealthFoodScore)
                + (0.20 * hungerScore)
                - (0.15 * deterrence);

        return Math.max(1.0, totalScore);
    }

    /**
     * Deterministically selects the target settlement using weighted random selection among eligible candidates.
     */
    public static Village selectTargetSettlement(
            MonsterLair lair,
            List<Village> villages,
            EcologicalRegionState regionState,
            List<Person> allPeople,
            long currentTotalMonths,
            Random random) {

        if (lair == null || villages == null || villages.isEmpty()) return null;

        List<Village> eligible = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        double totalWeight = 0.0;

        for (Village v : villages) {
            if (v == null || v.getLivingPopulationCount() <= 0) continue;

            // Enforce 24-month local invasion cooldown per settlement
            if (currentTotalMonths < (v.getLastMonsterTideMonth() + 24L)) {
                continue;
            }

            double score = computeAttractionScore(lair, v, regionState, allPeople);
            if (score > 0.0) {
                eligible.add(v);
                scores.add(score);
                totalWeight += score;
            }
        }

        if (eligible.isEmpty() || totalWeight <= 0.0) {
            return null;
        }

        if (random == null) return eligible.get(0);

        double roll = random.nextDouble() * totalWeight;
        double accum = 0.0;
        for (int i = 0; i < eligible.size(); i++) {
            accum += scores.get(i);
            if (roll <= accum) {
                return eligible.get(i);
            }
        }

        return eligible.get(eligible.size() - 1);
    }
}
